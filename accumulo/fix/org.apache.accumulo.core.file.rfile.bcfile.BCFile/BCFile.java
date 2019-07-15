

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.file.blockfile.impl.CachableBlockFile;
import org.apache.accumulo.core.file.rfile.bcfile.Compression;
import org.apache.accumulo.core.file.rfile.bcfile.MetaBlockAlreadyExists;
import org.apache.accumulo.core.file.rfile.bcfile.MetaBlockDoesNotExist;
import org.apache.accumulo.core.file.rfile.bcfile.Utils;
import org.apache.accumulo.core.file.streams.BoundedRangeFileInputStream;
import org.apache.accumulo.core.file.streams.PositionedDataOutputStream;
import org.apache.accumulo.core.file.streams.PositionedOutput;
import org.apache.accumulo.core.file.streams.SeekableDataInputStream;
import org.apache.accumulo.core.security.crypto.CryptoModule;
import org.apache.accumulo.core.security.crypto.CryptoModuleFactory;
import org.apache.accumulo.core.security.crypto.CryptoModuleParameters;
import org.apache.accumulo.core.security.crypto.SecretKeyEncryptionStrategy;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Seekable;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.compress.Compressor;
import org.apache.hadoop.io.compress.Decompressor;

import static org.apache.accumulo.core.file.rfile.bcfile.Utils.Version.size;


public final class BCFile {
	static final Utils.Version API_VERSION = new Utils.Version(((short) (2)), ((short) (0)));

	static final Utils.Version API_VERSION_1 = new Utils.Version(((short) (1)), ((short) (0)));

	static final Log LOG = LogFactory.getLog(BCFile.class);

	private static final String FS_OUTPUT_BUF_SIZE_ATTR = "tfile.fs.output.buffer.size";

	private static final String FS_INPUT_BUF_SIZE_ATTR = "tfile.fs.input.buffer.size";

	private static int getFSOutputBufferSize(Configuration conf) {
		return conf.getInt(BCFile.FS_OUTPUT_BUF_SIZE_ATTR, (256 * 1024));
	}

	private static int getFSInputBufferSize(Configuration conf) {
		return conf.getInt(BCFile.FS_INPUT_BUF_SIZE_ATTR, (32 * 1024));
	}

	private BCFile() {
	}

	public static class Writer implements Closeable {
		private final PositionedDataOutputStream out;

		private final Configuration conf;

		private final CryptoModule cryptoModule;

		private BCFile.BCFileCryptoModuleParameters cryptoParams;

		private SecretKeyEncryptionStrategy secretKeyEncryptionStrategy;

		final BCFile.DataIndex dataIndex;

		final BCFile.MetaIndex metaIndex;

		boolean blkInProgress = false;

		private boolean metaBlkSeen = false;

		private boolean closed = false;

		long errorCount = 0;

		private BytesWritable fsOutputBuffer;

		private interface BlockRegister {
			void register(long raw, long offsetStart, long offsetEnd);
		}

		private static final class WBlockState {
			private final Compression.Algorithm compressAlgo;

			private Compressor compressor;

			private final PositionedDataOutputStream fsOut;

			private final OutputStream cipherOut = null;

			private final long posStart;

			private OutputStream out;

			public WBlockState(Compression.Algorithm compressionAlgo, PositionedDataOutputStream fsOut, BytesWritable fsOutputBuffer, Configuration conf, CryptoModule cryptoModule, CryptoModuleParameters cryptoParams) throws IOException {
				this.compressAlgo = compressionAlgo;
				this.fsOut = fsOut;
				this.posStart = fsOut.position();
				fsOutputBuffer.setCapacity(BCFile.getFSOutputBufferSize(conf));
				cryptoParams.setCloseUnderylingStreamAfterCryptoStreamClose(false);
				cryptoParams.setRecordParametersToStream(false);
				cryptoParams.setInitializationVector(null);
				cryptoParams = cryptoModule.initializeCipher(cryptoParams);
				if ((cryptoParams.getInitializationVector()) != null) {
				}else {
				}
				cryptoParams = cryptoModule.getEncryptingOutputStream(cryptoParams);
				this.compressor = compressAlgo.getCompressor();
				try {
					this.out = compressionAlgo.createCompressionStream(cipherOut, compressor, 0);
				} catch (IOException e) {
					compressAlgo.returnCompressor(compressor);
					throw e;
				}
			}

			OutputStream getOutputStream() {
				return out;
			}

			long getCurrentPos() throws IOException {
				return 0L;
			}

			long getStartPos() {
				return posStart;
			}

			long getCompressedSize() throws IOException {
				long ret = (getCurrentPos()) - (posStart);
				return ret;
			}

			public void finish() throws IOException {
				try {
					if ((out) != null) {
						out.flush();
						out = null;
					}
				} finally {
					compressAlgo.returnCompressor(compressor);
					compressor = null;
				}
			}
		}

		public class BlockAppender extends DataOutputStream {
			private final BCFile.Writer.BlockRegister blockRegister;

			private final BCFile.Writer.WBlockState wBlkState;

			private boolean closed = false;

			BlockAppender(BCFile.Writer.BlockRegister register, BCFile.Writer.WBlockState wbs) {
				super(wbs.getOutputStream());
				this.blockRegister = register;
				this.wBlkState = wbs;
			}

			public long getRawSize() throws IOException {
				return (size()) & 4294967295L;
			}

			public long getCompressedSize() throws IOException {
				return wBlkState.getCompressedSize();
			}

			public long getStartPos() {
				return wBlkState.getStartPos();
			}

			@Override
			public void flush() {
			}

			@Override
			public void close() throws IOException {
				if ((closed) == true) {
					return;
				}
				try {
					++(errorCount);
					wBlkState.finish();
					blockRegister.register(getRawSize(), wBlkState.getStartPos(), wBlkState.getCurrentPos());
					--(errorCount);
				} finally {
					closed = true;
					blkInProgress = false;
				}
			}
		}

		public <OutputStreamType extends OutputStream & PositionedOutput> Writer(OutputStreamType fout, String compressionName, Configuration conf, boolean trackDataBlocks, AccumuloConfiguration accumuloConfiguration) throws IOException {
			if ((fout.position()) != 0) {
				throw new IOException("Output file not at zero offset.");
			}
			this.out = new PositionedDataOutputStream(fout);
			this.conf = conf;
			dataIndex = new BCFile.DataIndex(compressionName, trackDataBlocks);
			metaIndex = new BCFile.MetaIndex();
			fsOutputBuffer = new BytesWritable();
			BCFile.Magic.write(this.out);
			this.cryptoModule = CryptoModuleFactory.getCryptoModule(accumuloConfiguration);
			this.cryptoParams = new BCFile.BCFileCryptoModuleParameters();
			CryptoModuleFactory.fillParamsObjectFromConfiguration(cryptoParams, accumuloConfiguration);
			this.cryptoParams = ((BCFile.BCFileCryptoModuleParameters) (cryptoModule.generateNewRandomSessionKey(cryptoParams)));
			this.secretKeyEncryptionStrategy = CryptoModuleFactory.getSecretKeyEncryptionStrategy(accumuloConfiguration);
			this.cryptoParams = ((BCFile.BCFileCryptoModuleParameters) (secretKeyEncryptionStrategy.encryptSecretKey(cryptoParams)));
		}

		@Override
		public void close() throws IOException {
			if ((closed) == true) {
				return;
			}
			try {
				if ((errorCount) == 0) {
					if ((blkInProgress) == true) {
						throw new IllegalStateException("Close() called with active block appender.");
					}
					BCFile.Writer.BlockAppender appender = prepareMetaBlock(BCFile.DataIndex.BLOCK_NAME, getDefaultCompressionAlgorithm());
					try {
						dataIndex.write(appender);
					} finally {
						appender.close();
					}
					long offsetIndexMeta = out.position();
					metaIndex.write(out);
					if (((cryptoParams.getAlgorithmName()) == null) || (cryptoParams.getAlgorithmName().equals(Property.CRYPTO_CIPHER_SUITE.getDefaultValue()))) {
						out.writeLong(offsetIndexMeta);
						BCFile.API_VERSION_1.write(out);
					}else {
						long offsetCryptoParameters = out.position();
						cryptoParams.write(out);
						out.writeLong(offsetIndexMeta);
						out.writeLong(offsetCryptoParameters);
						BCFile.API_VERSION.write(out);
					}
					BCFile.Magic.write(out);
					out.flush();
				}
			} finally {
				closed = true;
			}
		}

		private Compression.Algorithm getDefaultCompressionAlgorithm() {
			return dataIndex.getDefaultCompressionAlgorithm();
		}

		private BCFile.Writer.BlockAppender prepareMetaBlock(String name, Compression.Algorithm compressAlgo) throws IOException, MetaBlockAlreadyExists {
			if ((blkInProgress) == true) {
				throw new IllegalStateException("Cannot create Meta Block until previous block is closed.");
			}
			if ((metaIndex.getMetaByName(name)) != null) {
			}
			BCFile.Writer.MetaBlockRegister mbr = new BCFile.Writer.MetaBlockRegister(name, compressAlgo);
			BCFile.Writer.WBlockState wbs = new BCFile.Writer.WBlockState(compressAlgo, out, fsOutputBuffer, conf, cryptoModule, cryptoParams);
			BCFile.Writer.BlockAppender ba = new BCFile.Writer.BlockAppender(mbr, wbs);
			blkInProgress = true;
			metaBlkSeen = true;
			return ba;
		}

		public BCFile.Writer.BlockAppender prepareMetaBlock(String name, String compressionName) throws IOException, MetaBlockAlreadyExists {
			return null;
		}

		public BCFile.Writer.BlockAppender prepareMetaBlock(String name) throws IOException, MetaBlockAlreadyExists {
			return prepareMetaBlock(name, getDefaultCompressionAlgorithm());
		}

		public BCFile.Writer.BlockAppender prepareDataBlock() throws IOException {
			if ((blkInProgress) == true) {
				throw new IllegalStateException("Cannot create Data Block until previous block is closed.");
			}
			if ((metaBlkSeen) == true) {
				throw new IllegalStateException("Cannot create Data Block after Meta Blocks.");
			}
			BCFile.Writer.DataBlockRegister dbr = new BCFile.Writer.DataBlockRegister();
			BCFile.Writer.WBlockState wbs = new BCFile.Writer.WBlockState(getDefaultCompressionAlgorithm(), out, fsOutputBuffer, conf, cryptoModule, cryptoParams);
			BCFile.Writer.BlockAppender ba = new BCFile.Writer.BlockAppender(dbr, wbs);
			blkInProgress = true;
			return ba;
		}

		private class MetaBlockRegister implements BCFile.Writer.BlockRegister {
			private final String name;

			private final Compression.Algorithm compressAlgo;

			MetaBlockRegister(String name, Compression.Algorithm compressAlgo) {
				this.name = name;
				this.compressAlgo = compressAlgo;
			}

			@Override
			public void register(long raw, long begin, long end) {
				metaIndex.addEntry(new BCFile.MetaIndexEntry(name, compressAlgo, new BCFile.BlockRegion(begin, (end - begin), raw)));
			}
		}

		private class DataBlockRegister implements BCFile.Writer.BlockRegister {
			DataBlockRegister() {
			}

			@Override
			public void register(long raw, long begin, long end) {
				dataIndex.addBlockRegion(new BCFile.BlockRegion(begin, (end - begin), raw));
			}
		}
	}

	private static final byte[] NO_CPYPTO_KEY = "ce18cf53c4c5077f771249b38033fa14bcb31cca0e5e95a371ee72daa8342ea2".getBytes(StandardCharsets.UTF_8);

	private static final BCFile.BCFileCryptoModuleParameters NO_CRYPTO = new BCFile.BCFileCryptoModuleParameters() {
		@Override
		public Map<String, String> getAllOptions() {
			return Collections.emptyMap();
		}

		@Override
		public byte[] getEncryptedKey() {
			return BCFile.NO_CPYPTO_KEY;
		}

		@Override
		public String getOpaqueKeyEncryptionKeyID() {
			return "NONE:a4007e6aefb095a5a47030cd6c850818fb3a685dc6e85ba1ecc5a44ba68b193b";
		}
	};

	private static class BCFileCryptoModuleParameters extends CryptoModuleParameters {
		public void write(DataOutput out) throws IOException {
			out.writeInt(getAllOptions().size());
			for (String key : getAllOptions().keySet()) {
				out.writeUTF(key);
				out.writeUTF(getAllOptions().get(key));
			}
			out.writeUTF(getOpaqueKeyEncryptionKeyID());
			out.writeInt(getEncryptedKey().length);
			out.write(getEncryptedKey());
		}

		public void read(DataInput in) throws IOException {
			Map<String, String> optionsFromFile = new HashMap<>();
			int numContextEntries = in.readInt();
			for (int i = 0; i < numContextEntries; i++) {
				optionsFromFile.put(in.readUTF(), in.readUTF());
			}
			CryptoModuleFactory.fillParamsObjectFromStringMap(this, optionsFromFile);
			setOpaqueKeyEncryptionKeyID(in.readUTF());
			int encryptedSecretKeyLength = in.readInt();
			byte[] encryptedSecretKey = new byte[encryptedSecretKeyLength];
			in.readFully(encryptedSecretKey);
			setEncryptedKey(encryptedSecretKey);
		}
	}

	public static class Reader implements Closeable {
		private static final String META_NAME = "BCFile.metaindex";

		private static final String CRYPTO_BLOCK_NAME = "BCFile.cryptoparams";

		private final SeekableDataInputStream in;

		private final Configuration conf;

		final BCFile.DataIndex dataIndex;

		final BCFile.MetaIndex metaIndex;

		final Utils.Version version;

		private BCFile.BCFileCryptoModuleParameters cryptoParams;

		private CryptoModule cryptoModule;

		private SecretKeyEncryptionStrategy secretKeyEncryptionStrategy;

		private static final class RBlockState {
			private final Compression.Algorithm compressAlgo;

			private Decompressor decompressor;

			private final BCFile.BlockRegion region;

			private final InputStream in;

			private volatile boolean closed;

			public <InputStreamType extends InputStream & Seekable> RBlockState(Compression.Algorithm compressionAlgo, InputStreamType fsin, BCFile.BlockRegion region, Configuration conf, CryptoModule cryptoModule, Utils.Version bcFileVersion, CryptoModuleParameters cryptoParams) throws IOException {
				this.compressAlgo = compressionAlgo;
				this.region = region;
				this.decompressor = compressionAlgo.getDecompressor();
				BoundedRangeFileInputStream boundedRangeFileInputStream = new BoundedRangeFileInputStream(fsin, this.region.getOffset(), this.region.getCompressedSize());
				InputStream inputStreamToBeCompressed = boundedRangeFileInputStream;
				if ((cryptoParams != null) && (cryptoModule != null)) {
					DataInputStream tempDataInputStream = new DataInputStream(boundedRangeFileInputStream);
					int ivLength = tempDataInputStream.readInt();
					byte[] initVector = new byte[ivLength];
					tempDataInputStream.readFully(initVector);
					cryptoParams.setInitializationVector(initVector);
					cryptoParams.setEncryptedInputStream(boundedRangeFileInputStream);
					cryptoParams.setCloseUnderylingStreamAfterCryptoStreamClose(false);
					cryptoParams.setRecordParametersToStream(false);
					cryptoParams = cryptoModule.getDecryptingInputStream(cryptoParams);
					inputStreamToBeCompressed = cryptoParams.getPlaintextInputStream();
				}
				try {
					this.in = compressAlgo.createDecompressionStream(inputStreamToBeCompressed, decompressor, BCFile.getFSInputBufferSize(conf));
				} catch (IOException e) {
					compressAlgo.returnDecompressor(decompressor);
					throw e;
				}
				closed = false;
			}

			public InputStream getInputStream() {
				return in;
			}

			public String getCompressionName() {
				return compressAlgo.getName();
			}

			public BCFile.BlockRegion getBlockRegion() {
				return region;
			}

			public void finish() throws IOException {
				synchronized(in) {
					if (!(closed)) {
						try {
							in.close();
						} finally {
							closed = true;
							if ((decompressor) != null) {
								try {
									compressAlgo.returnDecompressor(decompressor);
								} finally {
									decompressor = null;
								}
							}
						}
					}
				}
			}
		}

		public static class BlockReader extends DataInputStream {
			private final BCFile.Reader.RBlockState rBlkState;

			private boolean closed = false;

			BlockReader(BCFile.Reader.RBlockState rbs) {
				super(rbs.getInputStream());
				rBlkState = rbs;
			}

			@Override
			public void close() throws IOException {
				if ((closed) == true) {
					return;
				}
				try {
					rBlkState.finish();
				} finally {
					closed = true;
				}
			}

			public String getCompressionName() {
				return rBlkState.getCompressionName();
			}

			public long getRawSize() {
				return rBlkState.getBlockRegion().getRawSize();
			}

			public long getCompressedSize() {
				return rBlkState.getBlockRegion().getCompressedSize();
			}

			public long getStartPos() {
				return rBlkState.getBlockRegion().getOffset();
			}
		}

		public <InputStreamType extends InputStream & Seekable> Reader(InputStreamType fin, long fileLength, Configuration conf, AccumuloConfiguration accumuloConfiguration) throws IOException {
			this.in = new SeekableDataInputStream(fin);
			this.conf = conf;
			this.in.seek(((fileLength - (BCFile.Magic.size())) - (size())));
			version = new Utils.Version(this.in);
			BCFile.Magic.readAndVerify(this.in);
			if ((!(version.compatibleWith(BCFile.API_VERSION))) && (!(version.equals(BCFile.API_VERSION_1)))) {
				throw new RuntimeException("Incompatible BCFile fileBCFileVersion.");
			}
			long offsetIndexMeta = 0;
			long offsetCryptoParameters = 0;
			if (version.equals(BCFile.API_VERSION_1)) {
				this.in.seek((((fileLength - (BCFile.Magic.size())) - (size())) - ((Long.SIZE) / (Byte.SIZE))));
				offsetIndexMeta = this.in.readLong();
			}else {
				this.in.seek((((fileLength - (BCFile.Magic.size())) - (size())) - (2 * ((Long.SIZE) / (Byte.SIZE)))));
				offsetIndexMeta = this.in.readLong();
				offsetCryptoParameters = this.in.readLong();
			}
			this.in.seek(offsetIndexMeta);
			metaIndex = new BCFile.MetaIndex(this.in);
			if (!(version.equals(BCFile.API_VERSION_1))) {
				this.in.seek(offsetCryptoParameters);
				cryptoParams = new BCFile.BCFileCryptoModuleParameters();
				cryptoParams.read(this.in);
				this.cryptoModule = CryptoModuleFactory.getCryptoModule(cryptoParams.getAllOptions().get(Property.CRYPTO_MODULE_CLASS.getKey()));
				if (accumuloConfiguration.getBoolean(Property.CRYPTO_OVERRIDE_KEY_STRATEGY_WITH_CONFIGURED_STRATEGY)) {
					Map<String, String> cryptoConfFromAccumuloConf = accumuloConfiguration.getAllPropertiesWithPrefix(Property.CRYPTO_PREFIX);
					Map<String, String> instanceConf = accumuloConfiguration.getAllPropertiesWithPrefix(Property.INSTANCE_PREFIX);
					cryptoConfFromAccumuloConf.putAll(instanceConf);
					for (String name : cryptoParams.getAllOptions().keySet()) {
						if (!(name.equals(Property.CRYPTO_SECRET_KEY_ENCRYPTION_STRATEGY_CLASS.getKey()))) {
							cryptoConfFromAccumuloConf.put(name, cryptoParams.getAllOptions().get(name));
						}else {
							cryptoParams.setKeyEncryptionStrategyClass(cryptoConfFromAccumuloConf.get(Property.CRYPTO_SECRET_KEY_ENCRYPTION_STRATEGY_CLASS.getKey()));
						}
					}
					cryptoParams.setAllOptions(cryptoConfFromAccumuloConf);
				}
				this.secretKeyEncryptionStrategy = CryptoModuleFactory.getSecretKeyEncryptionStrategy(cryptoParams.getKeyEncryptionStrategyClass());
				cryptoParams = ((BCFile.BCFileCryptoModuleParameters) (secretKeyEncryptionStrategy.decryptSecretKey(cryptoParams)));
			}else {
				BCFile.LOG.trace("Found a version 1 file to read.");
			}
			BCFile.Reader.BlockReader blockR = getMetaBlock(BCFile.DataIndex.BLOCK_NAME);
			try {
				dataIndex = new BCFile.DataIndex(blockR);
			} finally {
				blockR.close();
			}
		}

		public <InputStreamType extends InputStream & Seekable> Reader(CachableBlockFile.Reader cache, InputStreamType fin, long fileLength, Configuration conf, AccumuloConfiguration accumuloConfiguration) throws IOException {
			this.in = new SeekableDataInputStream(fin);
			this.conf = conf;
			CachableBlockFile.BlockRead cachedMetaIndex = cache.getCachedMetaBlock(BCFile.Reader.META_NAME);
			CachableBlockFile.BlockRead cachedDataIndex = cache.getCachedMetaBlock(BCFile.DataIndex.BLOCK_NAME);
			CachableBlockFile.BlockRead cachedCryptoParams = cache.getCachedMetaBlock(BCFile.Reader.CRYPTO_BLOCK_NAME);
			if (((cachedMetaIndex == null) || (cachedDataIndex == null)) || (cachedCryptoParams == null)) {
				this.in.seek(((fileLength - (BCFile.Magic.size())) - (size())));
				version = new Utils.Version(this.in);
				BCFile.Magic.readAndVerify(this.in);
				if ((!(version.compatibleWith(BCFile.API_VERSION))) && (!(version.equals(BCFile.API_VERSION_1)))) {
					throw new RuntimeException("Incompatible BCFile fileBCFileVersion.");
				}
				long offsetIndexMeta = 0;
				long offsetCryptoParameters = 0;
				if (version.equals(BCFile.API_VERSION_1)) {
					this.in.seek((((fileLength - (BCFile.Magic.size())) - (size())) - ((Long.SIZE) / (Byte.SIZE))));
					offsetIndexMeta = this.in.readLong();
				}else {
					this.in.seek((((fileLength - (BCFile.Magic.size())) - (size())) - (2 * ((Long.SIZE) / (Byte.SIZE)))));
					offsetIndexMeta = this.in.readLong();
					offsetCryptoParameters = this.in.readLong();
				}
				this.in.seek(offsetIndexMeta);
				metaIndex = new BCFile.MetaIndex(this.in);
				if ((!(version.equals(BCFile.API_VERSION_1))) && (cachedCryptoParams == null)) {
					this.in.seek(offsetCryptoParameters);
					cryptoParams = new BCFile.BCFileCryptoModuleParameters();
					cryptoParams.read(this.in);
					if (accumuloConfiguration.getBoolean(Property.CRYPTO_OVERRIDE_KEY_STRATEGY_WITH_CONFIGURED_STRATEGY)) {
						Map<String, String> cryptoConfFromAccumuloConf = accumuloConfiguration.getAllPropertiesWithPrefix(Property.CRYPTO_PREFIX);
						Map<String, String> instanceConf = accumuloConfiguration.getAllPropertiesWithPrefix(Property.INSTANCE_PREFIX);
						cryptoConfFromAccumuloConf.putAll(instanceConf);
						for (String name : cryptoParams.getAllOptions().keySet()) {
							if (!(name.equals(Property.CRYPTO_SECRET_KEY_ENCRYPTION_STRATEGY_CLASS.getKey()))) {
								cryptoConfFromAccumuloConf.put(name, cryptoParams.getAllOptions().get(name));
							}else {
								cryptoParams.setKeyEncryptionStrategyClass(cryptoConfFromAccumuloConf.get(Property.CRYPTO_SECRET_KEY_ENCRYPTION_STRATEGY_CLASS.getKey()));
							}
						}
						cryptoParams.setAllOptions(cryptoConfFromAccumuloConf);
					}
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					DataOutputStream dos = new DataOutputStream(baos);
					cryptoParams.write(dos);
					dos.close();
					cache.cacheMetaBlock(BCFile.Reader.CRYPTO_BLOCK_NAME, baos.toByteArray());
					this.cryptoModule = CryptoModuleFactory.getCryptoModule(cryptoParams.getAllOptions().get(Property.CRYPTO_MODULE_CLASS.getKey()));
					this.secretKeyEncryptionStrategy = CryptoModuleFactory.getSecretKeyEncryptionStrategy(cryptoParams.getKeyEncryptionStrategyClass());
					cryptoParams = ((BCFile.BCFileCryptoModuleParameters) (secretKeyEncryptionStrategy.decryptSecretKey(cryptoParams)));
				}else
					if (cachedCryptoParams != null) {
						setupCryptoFromCachedData(cachedCryptoParams);
					}else {
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						DataOutputStream dos = new DataOutputStream(baos);
						BCFile.NO_CRYPTO.write(dos);
						dos.close();
						cache.cacheMetaBlock(BCFile.Reader.CRYPTO_BLOCK_NAME, baos.toByteArray());
					}

				if (cachedMetaIndex == null) {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					DataOutputStream dos = new DataOutputStream(baos);
					metaIndex.write(dos);
					dos.close();
					cache.cacheMetaBlock(BCFile.Reader.META_NAME, baos.toByteArray());
				}
				if (cachedDataIndex == null) {
					BCFile.Reader.BlockReader blockR = getMetaBlock(BCFile.DataIndex.BLOCK_NAME);
				}
				try {
					dataIndex = new BCFile.DataIndex(cachedDataIndex);
				} catch (IOException e) {
					BCFile.LOG.error("Got IOException when trying to create DataIndex block");
					throw e;
				} finally {
					cachedDataIndex.close();
				}
			}else {
				version = null;
				metaIndex = new BCFile.MetaIndex(cachedMetaIndex);
				dataIndex = new BCFile.DataIndex(cachedDataIndex);
				setupCryptoFromCachedData(cachedCryptoParams);
			}
		}

		private void setupCryptoFromCachedData(CachableBlockFile.BlockRead cachedCryptoParams) throws IOException {
			BCFile.BCFileCryptoModuleParameters params = new BCFile.BCFileCryptoModuleParameters();
			params.read(cachedCryptoParams);
			if ((Arrays.equals(params.getEncryptedKey(), BCFile.NO_CRYPTO.getEncryptedKey())) && (BCFile.NO_CRYPTO.getOpaqueKeyEncryptionKeyID().equals(params.getOpaqueKeyEncryptionKeyID()))) {
				this.cryptoParams = null;
				this.cryptoModule = null;
				this.secretKeyEncryptionStrategy = null;
			}else {
				this.cryptoModule = CryptoModuleFactory.getCryptoModule(params.getAllOptions().get(Property.CRYPTO_MODULE_CLASS.getKey()));
				this.secretKeyEncryptionStrategy = CryptoModuleFactory.getSecretKeyEncryptionStrategy(params.getKeyEncryptionStrategyClass());
				cryptoParams = ((BCFile.BCFileCryptoModuleParameters) (secretKeyEncryptionStrategy.decryptSecretKey(params)));
			}
		}

		public String getDefaultCompressionName() {
			return dataIndex.getDefaultCompressionAlgorithm().getName();
		}

		public Utils.Version getBCFileVersion() {
			return version;
		}

		public Utils.Version getAPIVersion() {
			return BCFile.API_VERSION;
		}

		@Override
		public void close() {
		}

		public int getBlockCount() {
			return dataIndex.getBlockRegionList().size();
		}

		public BCFile.Reader.BlockReader getMetaBlock(String name) throws IOException, MetaBlockDoesNotExist {
			BCFile.MetaIndexEntry imeBCIndex = metaIndex.getMetaByName(name);
			if (imeBCIndex == null) {
			}
			BCFile.BlockRegion region = imeBCIndex.getRegion();
			return createReader(imeBCIndex.getCompressionAlgorithm(), region);
		}

		public BCFile.Reader.BlockReader getDataBlock(int blockIndex) throws IOException {
			if ((blockIndex < 0) || (blockIndex >= (getBlockCount()))) {
				throw new IndexOutOfBoundsException(String.format("blockIndex=%d, numBlocks=%d", blockIndex, getBlockCount()));
			}
			BCFile.BlockRegion region = dataIndex.getBlockRegionList().get(blockIndex);
			return createReader(dataIndex.getDefaultCompressionAlgorithm(), region);
		}

		public BCFile.Reader.BlockReader getDataBlock(long offset, long compressedSize, long rawSize) throws IOException {
			BCFile.BlockRegion region = new BCFile.BlockRegion(offset, compressedSize, rawSize);
			return createReader(dataIndex.getDefaultCompressionAlgorithm(), region);
		}

		private BCFile.Reader.BlockReader createReader(Compression.Algorithm compressAlgo, BCFile.BlockRegion region) throws IOException {
			BCFile.Reader.RBlockState rbs = new BCFile.Reader.RBlockState(compressAlgo, in, region, conf, cryptoModule, version, cryptoParams);
			return new BCFile.Reader.BlockReader(rbs);
		}
	}

	static class MetaIndex {
		final Map<String, BCFile.MetaIndexEntry> index;

		public MetaIndex() {
			index = new TreeMap<>();
		}

		public MetaIndex(DataInput in) throws IOException {
			int count = Utils.readVInt(in);
			index = new TreeMap<>();
			for (int nx = 0; nx < count; nx++) {
				BCFile.MetaIndexEntry indexEntry = new BCFile.MetaIndexEntry(in);
				index.put(indexEntry.getMetaName(), indexEntry);
			}
		}

		public void addEntry(BCFile.MetaIndexEntry indexEntry) {
			index.put(indexEntry.getMetaName(), indexEntry);
		}

		public BCFile.MetaIndexEntry getMetaByName(String name) {
			return index.get(name);
		}

		public void write(DataOutput out) throws IOException {
			Utils.writeVInt(out, index.size());
			for (BCFile.MetaIndexEntry indexEntry : index.values()) {
				indexEntry.write(out);
			}
		}
	}

	static final class MetaIndexEntry {
		private final String metaName;

		private final Compression.Algorithm compressionAlgorithm;

		private static final String defaultPrefix = "data:";

		private final BCFile.BlockRegion region;

		public MetaIndexEntry(DataInput in) throws IOException {
			String fullMetaName = Utils.readString(in);
			if (fullMetaName.startsWith(BCFile.MetaIndexEntry.defaultPrefix)) {
				metaName = fullMetaName.substring(BCFile.MetaIndexEntry.defaultPrefix.length(), fullMetaName.length());
			}else {
				throw new IOException("Corrupted Meta region Index");
			}
			region = new BCFile.BlockRegion(in);
			compressionAlgorithm = null;
		}

		public MetaIndexEntry(String metaName, Compression.Algorithm compressionAlgorithm, BCFile.BlockRegion region) {
			this.metaName = metaName;
			this.compressionAlgorithm = compressionAlgorithm;
			this.region = region;
		}

		public String getMetaName() {
			return metaName;
		}

		public Compression.Algorithm getCompressionAlgorithm() {
			return compressionAlgorithm;
		}

		public BCFile.BlockRegion getRegion() {
			return region;
		}

		public void write(DataOutput out) throws IOException {
			Utils.writeString(out, ((BCFile.MetaIndexEntry.defaultPrefix) + (metaName)));
			Utils.writeString(out, compressionAlgorithm.getName());
			region.write(out);
		}
	}

	static class DataIndex {
		static final String BLOCK_NAME = "BCFile.index";

		private final Compression.Algorithm defaultCompressionAlgorithm;

		private final ArrayList<BCFile.BlockRegion> listRegions;

		private boolean trackBlocks;

		public DataIndex(DataInput in) throws IOException {
			int n = Utils.readVInt(in);
			listRegions = new ArrayList<>(n);
			for (int i = 0; i < n; i++) {
				BCFile.BlockRegion region = new BCFile.BlockRegion(in);
				listRegions.add(region);
			}
			defaultCompressionAlgorithm = null;
		}

		public DataIndex(String defaultCompressionAlgorithmName, boolean trackBlocks) {
			this.trackBlocks = trackBlocks;
			listRegions = new ArrayList<>();
			defaultCompressionAlgorithm = null;
		}

		public Compression.Algorithm getDefaultCompressionAlgorithm() {
			return defaultCompressionAlgorithm;
		}

		public ArrayList<BCFile.BlockRegion> getBlockRegionList() {
			return listRegions;
		}

		public void addBlockRegion(BCFile.BlockRegion region) {
			if (trackBlocks)
				listRegions.add(region);

		}

		public void write(DataOutput out) throws IOException {
			Utils.writeString(out, defaultCompressionAlgorithm.getName());
			Utils.writeVInt(out, listRegions.size());
			for (BCFile.BlockRegion region : listRegions) {
				region.write(out);
			}
		}
	}

	static final class Magic {
		private static final byte[] AB_MAGIC_BCFILE = new byte[]{ ((byte) (209)), ((byte) (17)), ((byte) (211)), ((byte) (104)), ((byte) (145)), ((byte) (181)), ((byte) (215)), ((byte) (182)), ((byte) (57)), ((byte) (223)), ((byte) (65)), ((byte) (64)), ((byte) (146)), ((byte) (186)), ((byte) (225)), ((byte) (80)) };

		public static void readAndVerify(DataInput in) throws IOException {
			byte[] abMagic = new byte[BCFile.Magic.size()];
			in.readFully(abMagic);
			if (!(Arrays.equals(abMagic, BCFile.Magic.AB_MAGIC_BCFILE))) {
				throw new IOException("Not a valid BCFile.");
			}
		}

		public static void write(DataOutput out) throws IOException {
			out.write(BCFile.Magic.AB_MAGIC_BCFILE);
		}

		public static int size() {
			return BCFile.Magic.AB_MAGIC_BCFILE.length;
		}
	}

	static final class BlockRegion {
		private final long offset;

		private final long compressedSize;

		private final long rawSize;

		public BlockRegion(DataInput in) throws IOException {
			offset = Utils.readVLong(in);
			compressedSize = Utils.readVLong(in);
			rawSize = Utils.readVLong(in);
		}

		public BlockRegion(long offset, long compressedSize, long rawSize) {
			this.offset = offset;
			this.compressedSize = compressedSize;
			this.rawSize = rawSize;
		}

		public void write(DataOutput out) throws IOException {
			Utils.writeVLong(out, offset);
			Utils.writeVLong(out, compressedSize);
			Utils.writeVLong(out, rawSize);
		}

		public long getOffset() {
			return offset;
		}

		public long getCompressedSize() {
			return compressedSize;
		}

		public long getRawSize() {
			return rawSize;
		}

		public long magnitude() {
			return offset;
		}
	}
}

