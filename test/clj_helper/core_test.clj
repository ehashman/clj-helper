(ns clj-helper.core-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :refer [make-parents resource]]
            [clojure.string :as s]
            [me.raynes.fs :refer [delete-dir executable? exists?]]
            [clj-helper.core :refer :all]))

;; TODO: Write great tests. Or something.

(deftest -main-tests
  (testing "-main works okay with no input"
    (with-redefs [read-line (constantly "")]
      (let [cwd (System/getProperty "user.dir")
            test-dir (str cwd "/output")
            test-project (slurp (resource "test/project.clj"))
            project-dst (str test-dir "/project.clj")]
        (make-parents project-dst)
        (spit project-dst test-project)
        (System/setProperty "user.dir" test-dir)
        (let [output (with-out-str (-main))]
          (println output)
          (are [msg] (s/includes? output msg)
            "Enter the project's short name (e.g. 'clj-http'): [output]"
            "Enter the source package's homepage: []"
            "Enter the year this release is copyrighted: []"
            "Enter the upstream author's name: []"
            "Enter the upstream author's email: []"
            "Enter the upstream license, in abbreviated form: [EPL-1.0]"
            "Enter the names of any dependencies, separated by commas: []"
            "Enter the package maintainer(s): [Debian Clojure Maintainers <pkg-clojure-maintainers@lists.alioth.debian.org>]"
            "Enter the package uploader(s): []"
            "Enter the project description, ending with a blank line:"
            "Generating pom..."
            "Moving pom to debian/pom.xml..."
            "Now you can create your Debian changelog with `dch --create`."
            "Once you have committed these changes to version control, you may build your package with `gbp buildpackage -uc -us`.")
          (are [f] (exists? f)
            "output/project.clj"
            "output/debian/compat"
            "output/debian/control"
            "output/debian/copyright"
            "output/debian/liboutput-clojure.classpath"
            "output/debian/liboutput-clojure.doc-base"
            "output/debian/liboutput-clojure.docs"
            "output/debian/liboutput-clojure.jlibs"
            "output/debian/liboutput-clojure.poms"
            "output/debian/pom.xml"
            "output/debian/rules"
            "output/debian/source/format")
          (is (executable? "output/debian/rules"))
          (delete-dir test-dir))))))
