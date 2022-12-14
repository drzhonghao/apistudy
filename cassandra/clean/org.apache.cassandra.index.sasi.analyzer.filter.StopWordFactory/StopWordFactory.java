import org.apache.cassandra.index.sasi.analyzer.filter.*;


import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a list of Stop Words for a given language
 */
public class StopWordFactory
{
    private static final Logger logger = LoggerFactory.getLogger(StopWordFactory.class);

    private static final String DEFAULT_RESOURCE_EXT = "_ST.txt";
    private static final String DEFAULT_RESOURCE_PREFIX = StopWordFactory.class.getPackage()
            .getName().replace(".", File.separator);
    private static final Set<String> SUPPORTED_LANGUAGES = new HashSet<>(
            Arrays.asList("ar","bg","cs","de","en","es","fi","fr","hi","hu","it",
            "pl","pt","ro","ru","sv"));

    private static final LoadingCache<String, Set<String>> STOP_WORDS_CACHE = CacheBuilder.newBuilder()
            .build(new CacheLoader<String, Set<String>>()
            {
                public Set<String> load(String s)
                {
                    return getStopWordsFromResource(s);
                }
            });

    public static Set<String> getStopWordsForLanguage(Locale locale)
    {
        if (locale == null)
            return null;

        String rootLang = locale.getLanguage().substring(0, 2);
        try
        {
            return (!SUPPORTED_LANGUAGES.contains(rootLang)) ? null : STOP_WORDS_CACHE.get(rootLang);
        }
        catch (ExecutionException e)
        {
            logger.error("Failed to populate Stop Words Cache for language [{}]", locale.getLanguage(), e);
            return null;
        }
    }

    private static Set<String> getStopWordsFromResource(String language)
    {
        Set<String> stopWords = new HashSet<>();
        String resourceName = DEFAULT_RESOURCE_PREFIX + File.separator + language + DEFAULT_RESOURCE_EXT;
        try (InputStream is = StopWordFactory.class.getClassLoader().getResourceAsStream(resourceName);
             BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)))
        {
                String line;
                while ((line = r.readLine()) != null)
                {
                    //skip comments (lines starting with # char)
                    if(line.charAt(0) == '#')
                        continue;
                    stopWords.add(line.trim());
                }
        }
        catch (Exception e)
        {
            logger.error("Failed to retrieve Stop Terms resource for language [{}]", language, e);
        }
        return stopWords;
    }
}
