package com.ksyun.plugin;

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
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author vercen
 * @version 1.0
 * @date 2023/7/12 13:05
 */

// 目标名统一为bootJar
@Mojo(name = "bootJar")
public class BootJarMojo extends AbstractMojo {
    // 可自由获取maven内置变量
    @Parameter(
            defaultValue = "${settings.localRepository}",
            required = true
    )
    private String localRepository;

    // 接收通过命令mvn -Dmain.class=com.ksyun.train.App传递的参数， 请勿修改参数名
    @Parameter(
            property = "main.class",
            required = true
    )
    private String mainClass;
    // maven项目信息，需要的数据基本都可以从此对象中获取，
    // 请自行调试打印观察project信息，开发过程中可利用json工具打印该对象信息
    @Component
    protected MavenProject project;

    //插件核心逻辑
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
        // get project dependency jars, ignore dependency transfer, only onedemo
        List<File> dependencyFiles = new ArrayList<File>();
        for (Artifact artifact : project.getDependencyArtifacts()) {
            dependencyFiles.add(artifact.getFile());
        }

        try {
            // Step 1: 创建一个临时目录
            File tempDir = new File(targetDirectory, "temp");
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }

            // Step 2: 创建一个可执行的JAR文件
            File jarFile = new File(tempDir, artifactId + "-" + version + ".jar");
            StringBuilder classPath = new StringBuilder();
            for (File dependencyFile : dependencyFiles) {
                classPath.append("lib/").append(dependencyFile.getName()).append(" ");
            }
            createExecutableJar(jarFile, classesDirectory, mainClass, classPath.toString());

            // Step 3: 复制所有依赖的JAR文件到临时目录中的lib子目录
            File libDir = new File(tempDir, "lib");
            if (!libDir.exists()) {
                libDir.mkdirs();
            }
            for (File dependencyFile : dependencyFiles) {
                Files.copy(dependencyFile.toPath(), new File(libDir, dependencyFile.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            // Step 4: 将可执行的JAR文件和lib目录一起打包成ZIP文件
            File zipFile = new File(targetDirectory, artifactId + "-" + version + ".zip");
            createZipFile(zipFile, tempDir);

            // Step 5: 删除不需要的文件和目录
            FileUtils.deleteDirectory(new File(targetDirectory, "generated-sources"));
            FileUtils.deleteDirectory(new File(targetDirectory, "maven-archiver"));
            FileUtils.deleteDirectory(new File(targetDirectory, "maven-status"));
            FileUtils.deleteDirectory(new File(targetDirectory, "surefire-reports"));
            FileUtils.deleteDirectory(new File(targetDirectory, "test-classes"));
            FileUtils.deleteDirectory(new File(targetDirectory,"generated-test-sources"));
            new File(targetDirectory, artifactId + "-" + version + ".jar").delete();

            // Step 6: 清理临时目录
            jarFile.delete();
            FileUtils.deleteDirectory(tempDir);

        } catch (Exception e) {
            throw new MojoFailureException("Failed to create bootJar", e);
        }
    }

    private void createExecutableJar(File jarFile, File classesDirectory, String mainClass, String classPath) throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, mainClass);
        manifest.getMainAttributes().put(Attributes.Name.CLASS_PATH, classPath);
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile), manifest)) {
            Files.walkFileTree(classesDirectory.toPath(), new SimpleFileVisitor<Path>() {
                @Override

                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path relativePath = classesDirectory.toPath().relativize(file);
                    JarEntry entry = new JarEntry(relativePath.toString().replace("\\", "/"));
                    jos.putNextEntry(entry);
                    Files.copy(file, jos);
                    jos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    private void createZipFile(File zipFile, File directoryToZip) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            Files.walkFileTree(directoryToZip.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path relativePath = directoryToZip.toPath().relativize(file);
                    ZipEntry entry = new ZipEntry(relativePath.toString());
                    zos.putNextEntry(entry);
                    Files.copy(file, zos);
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
}
