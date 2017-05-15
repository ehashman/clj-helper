(ns clj-helper.core
  (:require
   [clojure.string :as s]
   [environ.core :refer [env]]
   [clj-helper.debian :refer :all])
  (:gen-class))

(defn get-control-data
  "Parses existing control data to help autocomplete information."
  [control-data]
  (let [raw-homepage (re-find #"(?m)^Homepage: .*$" control-data)
        homepage (s/replace-first raw-homepage #"^Homepage: " "")
        raw-description (->> (s/split-lines control-data)
                             (drop-while
                              (complement #(re-find #"^Description: " %))))
        description (->> raw-description
                         (map #(s/replace-first % #"^Description: " ""))
                         (s/join "\n"))]
    {:homepage homepage
     :description description}))

(defn get-copyright-data
  "Parses existing copyright data to help autocomplete information."
  [copyright-data]
  (let [raw-copyright-str (re-find #"(?m)^Copyright: .*$" copyright-data)
        copyright-string (s/replace-first raw-copyright-str #"^Copyright: " "")
        [_ year author email]
        (re-find #"^(\d{4}) ([a-zA-Z ]+) <(.*)>$" copyright-string)]
    {:copyright-year year
     :upstream-author-name author
     :upstream-author-email email}))

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

        control-data
        (try (-> (str cwd "/debian/control") slurp get-control-data)
             (catch Exception _ nil))
        copyright-data
        (try (-> (str cwd "/debian/copyright") slurp get-copyright-data)
             (catch Exception _ nil))

        user-name (:debfullname env)
        user-email (:debemail env)

        ;; Upstream package info
        package-name
        (get-user-input! "Enter the project's short name (e.g. 'clj-http'):"
                         relative-cwd)
        homepage
        (get-user-input! "Enter the source package's homepage:"
                         (:homepage control-data))
        copyright-year
        (get-user-input! "Enter the year this release is copyrighted:"
                         (:copyright-year copyright-data))
        upstream-author-name
        (get-user-input! "Enter the upstream author's name:"
                         (:upstream-author-name copyright-data))
        upstream-author-email
        (when-let [email (get-user-input!
                          "Enter the upstream author's email:"
                          (:upstream-author-email copyright-data))]
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
        (or (:description control-data)
            (do
              (println "Enter the project description, ending with a blank line:")
              (loop [input (read-line)
                     desc ""]
                (if (s/blank? input)
                  desc
                  (recur (read-line) (str desc "\n" input))))))
        description (s/trim raw-description)]

    {:jar-name package-name
     :source-package-name (str package-name "-clojure")
     :package-name (str "lib" package-name "-clojure")
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
    (make-poms! user-data)
    (generate-pom!)
    (make-changelog!)
    (println)
    (println "Once you have committed these changes to version control, you may"
             "build your package with `gbp buildpackage -uc -us`.")
    (shutdown-agents)))  ;; Required to ensure the call to `sh` returns
                         ;; and doesn't wait 60s; see CLJ-959 for details
