(ns clj-helper.debian
  (:require
   [clojure.java.io :refer [file make-parents resource]]
   [clojure.java.shell :refer [sh]]
   [clojure.string :as s]
   [selmer.parser :as sp]))

;;; Helpers

(defn really-write-to-file!
  "Writes the output to a destination path, formatting the package name
  into the path if necessary."
  [package-name destination output]
  (let [real-destination (format destination package-name)]
    (make-parents real-destination)
    (spit real-destination output)))

(defn copy-file!
  "Copies a Debian build file to the destination path verbatim."
  [filename]
  (let [source (format "templates/%s" filename)
        destination (format "debian/%s" filename)]
    (really-write-to-file! nil destination (slurp (resource source)))))

(defn render-template!
  "Renders data into the src-template, writing it to the appropriate
  destination. Package name is formatted into the path, if necessary."
  [data src-template destination package-name]
  (let [template (slurp (resource src-template))
        output (sp/render template data)]
    (really-write-to-file! package-name destination output)))

(defn render-template-for-package-file!
  "Given a package file (i.e. a file with the format 
  debian/PACKAGENAME.filetype), automatically determine the template
  and destination paths before calling render-template!"
  [{:keys [package-name] :as user-data} filetype]
  (render-template! user-data
                    (format "templates/package.%s.j2" filetype)
                    (format "debian/%%s.%s" filetype)
                    package-name))

;;; Debian file generators

(defn make-compat!
  "Makes the debian/compat file."
  []
  (copy-file! "compat"))

(defn make-source!
  "Makes the debian/source/format file."
  []
  (copy-file! "source/format"))

(defn make-docs!
  "Makes the debian/PACKAGENAME.docs file."
  [{:keys [package-name]}]
  (let [source "templates/package.docs"
        destination "debian/%s.docs"]
    (really-write-to-file! package-name destination (slurp (resource source)))))

(defn make-classpath!
  "Makes the debian/PACKAGENAME.classpath file."
  [{:keys [package-name dependencies classpaths]}]
  (really-write-to-file! package-name
                         "debian/%s.classpath"
                         (s/join " " classpaths)))

(defn make-control!
  "Makes the debian/control file."
  [{:keys [dependencies] :as user-data}]
  (let [formatted-deps (->> dependencies
                            (map #(format " lib%s-clojure" %))
                            (s/join ",\n"))
        control-data (assoc user-data :dependencies formatted-deps)]
    (render-template! control-data "templates/control.j2" "debian/control" nil)))

(defn make-copyright!
  "Makes the debian/copyright file.

  Uses a generic copyright assignment for the debian/* files and
  licenses the packaging GPLv2+ by default."
  [user-data]
  (render-template! user-data "templates/copyright.j2" "debian/copyright" nil))

(defn make-rules!
  "Makes the debian/rules file and sets it as executable."
  [{:keys [classpaths] :as user-data}]
  (let [export-classpath (s/join ":" classpaths)
        rules-data (assoc user-data :export-classpath export-classpath)
        executable? true
        owner-only? false]
    (render-template! rules-data "templates/rules.j2" "debian/rules" nil)
    (.setExecutable (file "debian/rules") executable? owner-only?)))

(defn make-doc-base!
  "Makes the debian/PACKAGENAME.doc-base file."
  [user-data]
  (render-template-for-package-file! user-data "doc-base"))

(defn make-jlibs!
  "Makes the debian/PACKAGENAME.jlibs file."
  [user-data]
  (render-template-for-package-file! user-data "jlibs"))

(defn make-poms!
  "Makes the debian/PACKAGENAME.poms file."
  [user-data]
  (render-template-for-package-file! user-data "poms"))

(defn generate-pom!
  "Generates a pom for the leiningen project."
  []
  (println "Generating pom...")
  (println (:out (sh "lein" "pom")))
  (println "Moving pom to debian/pom.xml...")
  (.renameTo (file "pom.xml") (file "debian/pom.xml")))

(defn make-changelog!
  "Tells the user how to generate a changelog file.

  TODO: Maybe have the program shell out interactively? I got stuck
  trying to make that happen."
  []
  (println "Now you can create your Debian changelog with `dch --create`."))
