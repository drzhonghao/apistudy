import org.apache.karaf.shell.ssh.KnownHostsManager;
import org.apache.karaf.shell.ssh.*;


import java.io.IOException;
import java.net.SocketAddress;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

import org.apache.sshd.client.keyverifier.ServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;

public class ServerKeyVerifierImpl implements ServerKeyVerifier {

    private final KnownHostsManager knownHostsManager;
	private final boolean quiet;

    private final static String keyChangedMessage =
                    " @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ \n" +
                    " @    WARNING: REMOTE HOST IDENTIFICATION HAS CHANGED!      @ \n" +
                    " @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ \n" +
                    "IT IS POSSIBLE THAT SOMEONE IS DOING SOMETHING NASTY!\n" +
                    "Someone could be eavesdropping on you right now (man-in-the-middle attack)!\n" +
                    "It is also possible that the RSA host key has just been changed.\n" +
                    "Please contact your system administrator.\n" +
                    "Add correct host key in " + System.getProperty("user.home") + "/.sshkaraf/known_hosts to get rid of this message.\n" +
                    "Offending key in " + System.getProperty("user.home") + "/.sshkaraf/known_hosts\n" +
                    "RSA host key has changed and you have requested strict checking.\n" +
                    "Host key verification failed.";

    public ServerKeyVerifierImpl(KnownHostsManager knownHostsManager, boolean quiet) {
        this.knownHostsManager = knownHostsManager;
        this.quiet = quiet;
    }

    @Override
    public boolean verifyServerKey(ClientSession sshClientSession, SocketAddress remoteAddress,
                                   PublicKey serverKey) {
        PublicKey knownKey;
        try {
            knownKey = knownHostsManager.getKnownKey(remoteAddress, serverKey.getAlgorithm());
        } catch (InvalidKeySpecException e) {
            System.err.println("Invalid key stored for host " + remoteAddress + ". Terminating session.");
            return false;
        }
        if (knownKey == null) {
            boolean confirm;
            if (!quiet) {
                System.out.println("Connecting to unknown server. Add this server to known hosts ? (y/n)");
                confirm = getConfirmation();
            } else {
                System.out.println("Connecting to unknown server. Automatically adding to known hosts.");
                confirm = true;
            }
            if (confirm) {
                knownHostsManager.storeKeyForHost(remoteAddress, serverKey);
                System.out.println("Storing the server key in known_hosts.");
            } else {
                System.out.println("Aborting connection");
            }
            return confirm;
        }

        boolean verifed = (knownKey.equals(serverKey));
        if (!verifed) {
            System.err.println("Server key for host " + remoteAddress
                               + " does not match the stored key !! Terminating session.");
            System.err.println(keyChangedMessage);
        }
        return verifed;
    }

    private boolean getConfirmation() {
        int ch;
        try {
            do {
                ch = System.in.read();
            } while (ch != 'y' && ch != 'n');
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        boolean confirm = ch == 'y';
        return confirm;
    }

}
