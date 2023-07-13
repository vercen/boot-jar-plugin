# BootJarMoj插件

这是一个 Maven 插件，用于将 Java 项目打包成可执行的 JAR 文件和 ZIP 文件。该插件可通过 Maven 命令 `mvn com.ksyun.plugin:boot-jar-plugin:bootJar` 来调用。

## 使用方法

1. 在项目的 pom.xml 文件中添加以下插件配置：

   `````xml
   <build>
       <plugins>
           <plugin>
               <groupId>com.ksyun.plugin</groupId>
               <artifactId>boot-jar-plugin</artifactId>
               <version>1.0</version>
               <executions>
                   <execution>
                       <goals>
                           <goal>bootJar</goal>
                       </goals>
                   </execution>
               </executions>
               <configuration>
                   <main.class>com.example.MainClass</main.class>
               </configuration>
           </plugin>
       </plugins>
   </build>
   ```

   其中 `main.class` 参数指定了项目的主类名。

2. 在项目的根目录下执行 Maven 命令：

   ````
   mvn com.ksyun.plugin:boot-jar-plugin:bootJar
   ````
   如果没有在pom文件中指明主类，可以通过以下命令指定主类,并且打包：
   ````
   mvn clean -Dmain.class=com.example.MainClass package
     ````
   执行该命令后，插件将会自动执行以下操作：

    - 创建一个临时目录，用于存放生成的文件。
    - 创建一个可执行的 JAR 文件，包括项目编译后的 class 文件和所有依赖的 JAR 文件。
    - 复制所有依赖的 JAR 文件到临时目录中的 lib 子目录。
    - 将可执行的 JAR 文件和 lib 目录一起打包成 ZIP 文件。
    - 删除生成的中间文件。

   最终生成的文件将保存在 `target` 目录下。

## 注意事项

- 请确保项目的 pom.xml 文件中已经添加了所有依赖的 JAR 包。
- 请确保项目的主类已经在 pom.xml 文件中正确配置。
- 请确保项目中不存在包含敏感信息的文件，如密码、密钥等。
- 请在使用插件前仔细阅读插件代码，确保插件逻辑符合自己的需求。