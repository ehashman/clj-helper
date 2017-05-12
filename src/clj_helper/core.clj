(ns clj-helper.core
  (:require
   [clojure.string :as s]
   [environ.core :refer [env]]
   [clj-helper.debian :refer :all])
  (:gen-class))

(defn get-user-input!
  "Prints a direction msg for the user, displaying the default value
  chosen (if any), and requests their input."
  [msg default]
  (println (str msg " [" default "]"))
  (let [input (read-line)]
    (if (s/blank? input) default input)))

(defn get-user-data!
  "Prompts the user for all data required for generating packaging
  files."
  []
  (let [cwd (System/getProperty "user.dir")
        relative-cwd (re-find #"[^/]+$" cwd)
        user-name (:debfullname env)
        user-email (:debemail env)

        ;; Upstream package info
        package-name
        (get-user-input! "Enter the source package's name:" relative-cwd)
        homepage
        (get-user-input! "Enter the source package's homepage:" nil)
        copyright-year
        (get-user-input! "Enter the year this release is copyrighted:" nil)
        upstream-author-name
        (get-user-input! "Enter the upstream author's name:" nil)
        upstream-author-email
        (when-let [email (get-user-input! "Enter the upstream author's email:"
                                          nil)]
          (str "<" email ">"))
        upstream-license
        (get-user-input! "Enter the upstream license, in abbreviated form:"
                         "EPL-1.0")

        ;; Dependencies and build stuff
        raw-dependencies
        (get-user-input!
         "Enter the names of any dependencies, separated by commas:"
         "")  ;; avoid NPEs on s/split
        dependencies (->> (s/split raw-dependencies #",")
                          (map s/trim)
                          (remove s/blank?))
        full-deps (cons "clojure" dependencies)
        classpaths (map #(format "/usr/share/java/%s.jar" %) full-deps)

        ;; Package-specific info
        maintainer
        (get-user-input!
         "Enter the package maintainer(s):"
         "Debian Clojure Maintainers <pkg-clojure-maintainers@lists.alioth.debian.org>")
        uploaders
        (get-user-input! "Enter the package uploader(s):"
                         (and user-name
                              user-email
                              (format "%s <%s>" user-name user-email)))
        raw-description
        (do
          (println "Enter the project description, ending with a blank line:")
          (loop [input (read-line)
                 desc ""]
            (if (s/blank? input)
              desc
              (recur (read-line) (str desc "\n" input)))))
        description (s/trim raw-description)]

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
  "When run in the root of a Clojure repository, packaged with
  Leiningen, generates a debian/ directory and all necessary files
  for packaging."
  [& args]
  (let [user-data (get-user-data!)]
    (make-compat!)
    (make-source!)
    (make-docs! user-data)
    (make-classpath! user-data)
    (make-control! user-data)
    (make-copyright! user-data)
    (make-rules! user-data)
    (make-doc-base! user-data)
    (make-jlibs! user-data)
    (make-poms! user-data)
    (generate-pom!)
    (make-changelog!)
    (println)
    (println "Once you have committed these changes to version control, you may"
             "build your package with `gbp buildpackage -uc -us`.")
    (shutdown-agents)))  ;; Required to ensure the call to `sh` returns
                         ;; and doesn't wait 60s; see CLJ-959 for details
