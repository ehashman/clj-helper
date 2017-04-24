(ns clj-helper.core
  (:require
   [clojure.java.io :refer [file make-parents resource]]
   [clojure.java.shell :refer [sh]]
   [clojure.string :as s]
   [selmer.parser :as sp])
  (:gen-class))

(defn get-user-input!
  [msg default]
  (println (str msg " [" default "]"))
  (let [input (read-line)]
    (if (s/blank? input) default input)))

(defn really-write-to-file!
  [package-name destination output]
  (let [real-destination (format destination package-name)]
    (make-parents real-destination)
    (spit real-destination output)))

(defn render-template!
  [data template-src destination package-name]
  (let [template (slurp (resource template-src))
        output (sp/render template data)]
    (really-write-to-file! package-name destination output)))

(defn render-template-for-package-file!
  [{:keys [package-name] :as user-data} filetype]
  (render-template! user-data
                    (format "templates/package.%s.j2" filetype)
                    (format "debian/%%s.%s" filetype)
                    package-name))

(defn copy-file!
  [filename]
  (let [source (format "templates/%s" filename)
        destination (format "debian/%s" filename)]
    (really-write-to-file! nil destination (slurp (resource source)))))

(defn make-control!
  [user-data]
  (let [control-data (assoc user-data :dep-separator ",\n")]
    (render-template! control-data "templates/control.j2" "debian/control" nil)))

(defn make-copyright!
  [user-data]
  (render-template! user-data "templates/copyright.j2" "debian/copyright" nil))

(defn make-classpath!
  [{:keys [package-name dependencies classpaths]}]
  (really-write-to-file! package-name
                         "debian/%s.classpath"
                         (s/join " " classpaths)))

(defn make-doc-base!
  [user-data]
  (render-template-for-package-file! user-data "doc-base"))

(defn make-docs!
  [{:keys [package-name]}]
  (let [source "templates/package.docs"
        destination "debian/%s.docs"]
    (really-write-to-file! package-name destination (slurp (resource source)))))

(defn make-jlibs!
  [user-data]
  (render-template-for-package-file! user-data "jlibs"))

(defn generate-pom!
  []
  (println "Generating pom...")
  (println (:out (sh "lein" "pom")))
  (println "Moving pom to debian/pom.xml...")
  (.renameTo (file "pom.xml") (file "debian/pom.xml")))

(defn make-poms!
  [user-data]
  (render-template-for-package-file! user-data "poms"))

(defn make-rules!
  [{:keys [classpaths] :as user-data}]
  (let [export-classpath (s/join ":" classpaths)
        rules-data (assoc user-data :export-classpath export-classpath)
        executable? true
        owner-only? false]
    (render-template! rules-data "templates/rules.j2" "debian/rules" nil)
    (.setExecutable (file "debian/rules") executable? owner-only?)))

(defn make-compat!
  []
  (copy-file! "compat"))

(defn make-source!
  []
  (copy-file! "source/format"))

(defn make-changelog!
  []
  (println "Now you can create your Debian changelog with `dch --create`."))

(defn get-user-data!
  []
  (let [cwd (System/getProperty "user.dir")
        relative-cwd (re-find #"[^/]+$" cwd)
        package-name
        (get-user-input! "Enter the source package's name:" relative-cwd)
        raw-dependencies
        (-> (get-user-input!
             "Enter the names of any dependencies, separated by commas:"
             "")
            (s/split #","))
        dependencies (->> raw-dependencies
                          (map s/trim)
                          (remove s/blank?))
        full-deps (cons "clojure" dependencies)
        classpaths (map #(format "/usr/share/java/%s.jar" %) full-deps)
        maintainer
        (get-user-input!
         "Enter the package maintainer(s):"
         "Debian Java Maintainers <pkg-java-maintainers@lists.alioth.debian.org>")
        uploaders
        (get-user-input! "Enter the package uploader(s):"
                         "Elana Hashman <debian@hashman.ca>")
        homepage
        (get-user-input! "Enter the project's homepage:" nil)
        raw-description
        (do
          (println "Enter the project description, ending with a blank line:")
          (loop [input (read-line)
                 desc ""]
           (if (s/blank? input)
             desc
             (recur (read-line) (str desc "\n" input)))))
        description (s/trim raw-description)
        copyright-year
        (get-user-input! "Enter the year the project is copyrighted:" nil)
        upstream-author-name
        (get-user-input! "Enter the upstream author's name:" nil)
        upstream-author-email
        (get-user-input! "Enter the upstream author's email:" nil)
        upstream-license
        (get-user-input! "Enter the upstream license, in abbreviated form:"
                         "EPL-1.0")]

     {:package-name package-name
      :dependencies dependencies
      :classpaths classpaths
      :maintainer maintainer
      :uploaders uploaders
      :homepage homepage
      :description description
      :copyright-year copyright-year
      :upstream-author-name upstream-author-name
      :upstream-author-email upstream-author-email
      :upstream-license upstream-license}))

(defn -main
  [& args]
  (let [user-data (get-user-data!)]
    (make-control! user-data)
    (make-copyright! user-data)
    (make-classpath! user-data)
    (make-doc-base! user-data)
    (make-docs! user-data)
    (make-jlibs! user-data)
    (generate-pom!)
    (make-poms! user-data)
    (make-rules! user-data)
    (make-compat!)
    (make-source!)
    (make-changelog!)
    (println)
    (println "Once you have committed these changes to version control, you may"
             "build your package with `gbp gbp buildpackage -uc -us`.")
    (shutdown-agents)))
