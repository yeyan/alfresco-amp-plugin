alfresco-amp-plugin
===================

A gradle pluin that pack project into an Alfresco AMP file


how to use
----------

navigate to a directory you comfortable with, then issue the following commands:

```shell
git clone https://github.com/yeyan/alfresco-amp-plugin.git

cd amp-plugin

# this installs the plugin into your local maven repository, 
# you can change the build.gradle to install into anther location
gradle publish 
```

Then in you build script add the following:

```groovy

buildscript {
    repositories {
        mavenLocal()
    }
    dependencies {
        classpath group: 'com.parashift.amp', name: 'amp-plugin', version: '1.0-SNAPSHOT'
    }
}

apply-plugin: 'alfresco-amp'

amp {
    // id is mandatory when you want to pack a AMP
    id = "com.parashift.amp"  

    // the following is options but highly recommended you fill yourself
    // as the plugin just infer the values from project.version 
    // and project.name 
    alias = "alfresco-amp"
    version = "1.0.0"
    title = "Alfresco AMP Packer"
    description = "An aflresco AMP Module"

    // depends on dynamic extension will enable bundle packing mode 
    // you jar file will to copied to /bundle instead of /lib and
    // a file-mapping properties will be generated for /bundle
    dependsOn("com.github.dynamicextensionsalfresco:1.0:2.0") 

    // will generate module.depends.some.other.dependency.C=2.2-*
    dependsOn("some.other.dependency.A:2.2:") 

    // will generate module.depends.some.other.dependency.C=*-2.2
    dependsOn("some.other.dependency.B::2.2") 

    // will generate module.depends.some.other.dependency.C=*-*
    dependsOn("some.other.dependency.C::") 

    // extra jar files you want to copy to /WEB-INF/lib during install
    addLibDir("extraLibs") 
}
```

After all that you can pack you project into AMP file by issue 

```shell

gradle amp

```

gradle project group
--------------------

Alfresco AMP plugin can be also used with gradle project groups. But with the following limitations:

1. properties like id, alias, title, description will be ignored on subprojects,
   dependencies and extra libraries can be configured in subprojects as normal.

2. AMP file can be only packed with root project. Issuing amp command in subprojects will
   not deliver any AMP file
