package br.app.mastersofthouse.report.tools.classpath;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;

public class ApplicationClasspath {

    private static final Class<?>[] parameterTypes = new Class[]{URL.class};
    public static final int FIND_FROM_CLASSPATH = 2;
    public static final int FIND_FROM_THIS = 1;
    private static int defaultBasedirMethod = FIND_FROM_THIS;

    public static void add(String filename) throws IOException {
        File file = new File(filename);
        add(file);
    }
    public static void add(File file) throws IOException {
        add(file.toURI().toURL());
    }
    public static void add(URL url) throws IOException {
        URLClassLoader systemClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Class<?> urlClassLoaderClass = URLClassLoader.class;
        try {
            Method method = urlClassLoaderClass.getDeclaredMethod("addURL", parameterTypes);
            method.setAccessible(true);
            method.invoke(systemClassLoader, new Object[]{url});
        } catch (Exception e) {
            throw new IOException("Error, could not add URL to system classloader!");
        }
    }

    public static void addRelative(String relativeFilename) throws IOException, URISyntaxException {
        File file = new File(getAppBaseDir(), relativeFilename);
        add(file);
    }

    public static void addRelative(File relativeFile) throws IOException, URISyntaxException {
        File file = new File(getAppBaseDir(), relativeFile.getPath());
        add(file);
    }

    public static void addJars(String dirName) throws IOException {
        addJars(new File(dirName));
    }

    public static void addJars(File dir) throws IOException, FileNotFoundException {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            for (File file : files) {
                if (file.toString().toLowerCase().endsWith(".jar")) {
                    add(file);
                }
            }
        } else {
            throw new FileNotFoundException("Error, " + dir + " is not a directory!");
        }
    }

    public static void addJarsRelative(String dirName) throws IOException, URISyntaxException {
        addJars(new File(getAppBaseDir(), dirName));
    }

    public static void addJarsRelative(File dir) throws IOException, URISyntaxException {
        addJars(new File(getAppBaseDir(), dir.getPath()));
    }
    public static File getAppBaseDir() throws URISyntaxException {
        return getAppBaseDir(defaultBasedirMethod);
    }

    public static File getAppBaseDir(int basedirMethod) throws URISyntaxException {
        File returnval = null;
        switch (basedirMethod) {
            case FIND_FROM_THIS:
                returnval = getAppBaseDirFromThis();
                break;
            case FIND_FROM_CLASSPATH:
                returnval = getAppBaseDirFromClasspath();
                break;
        }
        return returnval;
    }

    public static void setDefaultBasedirMethod(int newDefaultBasedirMethod) {
        defaultBasedirMethod = newDefaultBasedirMethod;
    }

    public static File getAppBaseDirFromThis() throws URISyntaxException {
        URL baseUrl = ApplicationClasspath.class.getProtectionDomain().getCodeSource().getLocation();
        File baseDir;
        if (baseUrl.getAuthority() != null) {
            // workaroud Windows UNC path problems
            URI uri;
            uri = new URI(baseUrl.toURI().toString().replace("file://", "file:/"));
            baseDir = new File(File.separator + (new File(uri)).toString());
        } else {
            baseDir = new File(baseUrl.toURI());
        }
        if (!baseDir.isDirectory()) {
            baseDir = baseDir.getParentFile();
        }
        return baseDir;
    }

    public static File getAppBaseDirFromClasspath() throws URISyntaxException {
        URL baseUrl = ((URLClassLoader) ApplicationClasspath.class.getClassLoader()).getURLs()[0];
        File baseDir;
        if (baseUrl.getAuthority() != null) {
            URI uri;
            uri = new URI(baseUrl.toURI().toString().replace("file://", "file:/"));
            baseDir = new File(File.separator + (new File(uri)).toString());
        } else {
            baseDir = new File(baseUrl.toURI());
        }
        if (!baseDir.isDirectory()) {
            baseDir = baseDir.getParentFile();
        }
        return baseDir;
    }
}
