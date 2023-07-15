package com.ksyun.train.plugins;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author vercen
 * @version 1.0
 * @date 2023/7/12 13:05
 */

/**
 * BootJarMojo是一个Maven插件，它创建了一个可启动的JAR文件，其中包含了所有项目依赖。
 */
@Mojo(name = "bootJar")
public class BootJarMojo extends AbstractMojo {

    private static final String LIB_DIR_NAME = "lib";
    private static final String ZIP_FILE_EXTENSION = ".zip";
    private static final String JAR_FILE_EXTENSION = ".jar";

    @Parameter(
            defaultValue = "${settings.localRepository}",
            required = true
    )
    private String localRepository;

    @Parameter(
            property = "main.class",
            required = true
    )
    private String mainClass;

    @Component
    protected MavenProject project;

    @Override
    public void execute() throws MojoFailureException {
        getLog().info("project localRepository is " + localRepository);
        File baseDir = project.getBasedir();
        getLog().info("project base dir is " + baseDir);
        String artifactId = project.getArtifactId();
        String version = project.getVersion();
        File targetDirectory = new File(baseDir, "target");
        File classesDirectory = new File(targetDirectory, "classes");
        getLog().info("project classes dir is " + classesDirectory.getAbsolutePath());

        List<File> dependencyFiles = project.getDependencyArtifacts().stream().map(Artifact::getFile).collect(Collectors.toList());

        try {
            // 创建临时目录
            File tempDir = Files.createDirectories(targetDirectory.toPath().resolve("temp")).toFile();

            // 创建可执行的JAR文件并指定依赖的classpath
            File jarFile = new File(tempDir, artifactId + "-" + version + JAR_FILE_EXTENSION);
            String classPath = dependencyFiles.stream().map(f -> LIB_DIR_NAME + "/" + f.getName()).collect(Collectors.joining(" "));
            createExecutableJar(jarFile, classesDirectory, mainClass, classPath);

            // 将依赖文件复制到lib目录下
            File libDir = Files.createDirectories(tempDir.toPath().resolve(LIB_DIR_NAME)).toFile();
            for (File dependencyFile : dependencyFiles) {
                Files.copy(dependencyFile.toPath(), libDir.toPath().resolve(dependencyFile.getName()), StandardCopyOption.REPLACE_EXISTING);
            }

            // 创建zip文件
            File zipFile = new File(targetDirectory, artifactId + "-" + version + ZIP_FILE_EXTENSION);
            createZipFile(zipFile, tempDir);

            // 删除生成的目录和文件
            String[] directoriesToDelete = {"generated-sources", "maven-archiver", "maven-status", "surefire-reports", "test-classes", "generated-test-sources"};
            for (String directory : directoriesToDelete) {
                FileUtils.deleteDirectory(new File(targetDirectory, directory));
            }
            new File(targetDirectory, artifactId + "-" + version + JAR_FILE_EXTENSION).delete();

            // 删除临时目录和文件
            Files.deleteIfExists(jarFile.toPath());
            FileUtils.deleteDirectory(tempDir);

        } catch (Exception e) {
            throw new MojoFailureException("Failed to create bootJar", e);
        }
    }

    /**
     * 创建可执行的JAR文件
     *
     * @param jarFile          JAR文件
     * @param classesDirectory 项目类文件目录
     * @param mainClass        主类
     * @param classPath        依赖的classpath
     * @throws IOException
     */
    private void createExecutableJar(File jarFile, File classesDirectory, String mainClass, String classPath) throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, mainClass);
        manifest.getMainAttributes().put(Attributes.Name.CLASS_PATH, classPath);

        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarFile.toPath()), manifest)) {
            // 遍历项目类文件并添加到JAR文件中
            Files.walk(classesDirectory.toPath())
                    .filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            Path relativePath = classesDirectory.toPath().relativize(file);
                            JarEntry entry = new JarEntry(relativePath.toString().replace("\\", "/"));
                            jos.putNextEntry(entry);
                            Files.copy(file, jos);
                            jos.closeEntry();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }

    /**
     * 创建zip文件
     *
     * @param zipFile         zip文件
     * @param directoryToZip  需要压缩的目录
     * @throws IOException
     */
    private void createZipFile(File zipFile, File directoryToZip) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            // 遍历目录下的文件并添加到zip文件中
            Files.walk(directoryToZip.toPath())
                    .filter(Files::isRegularFile)
                    .forEach(f -> {
                        try {
                            Path relativePath = directoryToZip.toPath().relativize(f);
                            ZipEntry entry = new ZipEntry(relativePath.toString());
                            zos.putNextEntry(entry);
                            Files.copy(f, zos);
                            zos.closeEntry();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }
}