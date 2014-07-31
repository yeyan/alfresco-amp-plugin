/*
 * @Author Ye Yan (ye.yan@parashift.com.au)
 */

package com.parashift.amp.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.tooling.BuildException

class AmpPlugin implements Plugin<Project> {
    void apply(Project project) {
        applyPlugin(project)
    }

    void applyPlugin(Project project) {
        configureExtensions(project)
        configurePlugins(project)
        configureAmpTask(project)
    }

    void configureAmpTask(Project project) {
        def task = project.tasks.create("amp");
        task.dependsOn("build")

        task.ext.isRootProject = project.rootProject == project

        if(task.ext.isRootProject) {
            project.subprojects { subproj ->
                applyPlugin(subproj)
                task.dependsOn(":${subproj.name}:amp")
            }
        }

        configureTaskExt(project, task.ext)

        task << {
            // initialize AMP structures we possibly need
            ampSubdirs.each { dir ->
                if(!dir.exists()) dir.mkdirs()
            }
            
            // generate file mapping
            createFileMapping(fileMapping)

            def copyJarFiles = { fromDir , toDir ->
                if(fromDir.exists()) {

                    if(!toDir.exists()) {
                        toDir.mkdirs()
                    }

                    ant.copy (toDir: toDir, flatten: true) {
                        fileset (dir: fromDir) {
                            include (name: "**/*jar")
                        }
                    }
                }
            }

            // only generate module.properties for root project
            if(isRootProject) {
                project.subprojects { subproj ->
                    if(subproj.amp.dependencies) {
                        project.amp.dependencies.addAll(subproj.amp.dependencies)
                    }
                }

                project.amp.dependencies = project.amp.dependencies.unique { a, b -> 
                    a.id.compareTo(b.id)
                }

                createModuleProperties(project, moduleProperties)
            }

            // copy extra libraries to amp/lib directory
            project.amp.libDirs.findAll { it.exists() }.each { dir ->
                copyJarFiles(dir, libDir)
            }

            // copy project jar to amp/bundle or amp/lib depends on the type of the project
            ant.copy (file: project.jar.archivePath, todir: isBundleBased(project) ? bundleDir: libDir)

            // copy jar files from subproject's amp lib and bundle directory
            if(isRootProject) {
                project.subprojects { subproj ->
                    def subAmp = configureTaskExt(subproj, [:])

                    copyJarFiles(subAmp.libDir, libDir)
                    copyJarFiles(subAmp.bundleDir, bundleDir)
                }
            }

            // delete empty directories we don't need
            ampSubdirs.each { dir ->
                if(dir.list().length == 0 && dir.isDirectory()) dir.delete()
            }

            // delete file mapping if we don't have bundle folder
            if(!bundleDir.exists()) {
                fileMapping.delete()
            }

            // pack the amp
            if(isRootProject) {
                ant.zip(destfile: project.amp.archivePath, basedir: buildDir, update: true)
            }
        }

    }

    void configureExtensions(Project project) {
        project.extensions.create("amp", AmpExtension)

        project.amp.project = project
        project.amp.alias = project.amp.alias ?: project.name
        project.amp.version = project.amp.version ?: (project.version == "unspecified" ? "0.1.0" : project.version)
        project.amp.title = project.amp.title ?: project.name
        project.amp.description = project.amp.description == null ? "No description avaiable" : project.amp.description;

        project.amp.baseName = project.amp.baseName ?: project.amp.alias;
        project.amp.destinationDir = new File(project.buildDir, "amp")
    }

    void configurePlugins(Project project) {
        project.apply plugin: "java"
    }

    def configureTaskExt(project, ext) {
        ext.buildDir = new File(new File(project.buildDir, "tmp"), "amp")

        ext.moduleProperties = new File(ext.buildDir, "module.properties")
        ext.fileMapping = new File(ext.buildDir, "file-mapping.properties")

        ext.bundleDir = new File(ext.buildDir, "bundle")
        ext.libDir = new File(ext.buildDir, "lib")

        ext.ampSubdirs = [ext.bundleDir, ext.libDir]

        return ext
    }

    void createModuleProperties(Project project, File file) {
        if(!project.amp.id) {
            throw new BuildException("You must specify a module ID when you pack an AMP", null);
        }

        def props = [ "id=${project.amp.id}"
        , "aliases=${project.amp.alias}"
        , "version=${project.amp.version}"
        , "title=${project.amp.title}"
        , "description=${project.amp.description}"
        ].collect { "module.$it" }

        props.addAll(project.amp.dependencies.collect{"module.depends.${it.id}=${it.min}-${it.max}"});
        writeToFile(props.join("\n") + "\n", file)

    }

    void createDirectoryIfNotExist(File dir, String errorMsg) {
        if (!dir.exists()) {
            if(!dir.mkdirs()) throw new BuildException(errorMsg, null)
        }
    }

    void writeToFile(String content, File file) {
        def output = new FileWriter(file)

        try {
            output.write(content)
        } finally {
            output.close()
        }
    }

    void createFileMapping(File file) {
        def content = [ "/bundle=/WEB-INF/classes/alfresco/module/com.github.dynamicextensionsalfresco/standard-bundles" ].join("\n") + "\n"
        writeToFile(content, file)
    }

    boolean isBundleBased(Project project) {
        boolean result = false;
        project.amp.dependencies.each { if (it.id == "com.github.dynamicextensionsalfresco") result = true } 

        return result
    }

}

class AmpExtension {

    Project project

    def String baseName
    def File destinationDir 
    def File archivePath

    def libDirs = []

    def addLibDir(String path) {
        libDirs.add(new File(project.rootDir, path))
    }

    def String id
    def String alias
    def String version 
    def String title
    def String description

    def setVersion(String vers) {
        this.version = vers
        updateArchivePath()
    }

    def setDestinationDir(File dir) {
        this.destinationDir = dir
        updateArchivePath()
    }

    def setBaseName(String dir) {
        this.baseName = dir
        updateArchivePath()
    }

    def dependencies = []

    def dependsOn(String desc) {
        def match = desc =~ /([^:]+):([0-9.]*):([0-9.]*)/

        if(match.matches()){
            def dependency = [:]

            dependency.id  = match [0][1]
            dependency.min = match [0][2] == "" ? "*" : match [0][2]
            dependency.max = match [0][3] == "" ? "*" : match [0][3]

            dependencies.add(dependency)
        }else{
            throw new BuildException("invalid module dependency description: '$desc'", null)
        }

    }

    protected void updateArchivePath() {
        if(this.destinationDir && this.version) {
            this.archivePath = new File(this.destinationDir, "${baseName}-${version}.amp")
        }
    }
}

