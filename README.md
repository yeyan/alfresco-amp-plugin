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

gradle publish # this installs the plugin into your local maven repository, you can change the build.gradle to install into anther location
```

Then in you build script add the following:

```groovy

buildscript {
    repositories {
        mavenLocal()
    }
    dependencies {
        classpath group: 'com.github.amp', name: 'amp-plugin', version: '1.0-SNAPSHOT'
    }
}

apply-plugin: 'alfresco-amp'
```

After all that you can pack you project into AMP file by issue 

```shell

gradle amp

```
