# clj-helper

A utility for helping package Leiningen Clojure projects for Debian.

## Installation

Build from source:

```
$ lein uberjar
```

Alternatively, download the pre-built standalone jar.

You will need a debian build environment for actually building any packages
(providing `dch`, `gbp`, etc.)

## Usage

Run the standalone jar in the root of the repository you want to package:

```
~/src/target-repo-root$ java -jar clj-helper-0.1.0-standalone.jar
```

## Examples

Here is how I might use this tool to package and build an existing Clojure
project.

```
ehashman@corn-syrup:~/debian/quoin$ java -jar ../clj-helper-0.1.0-standalone.jar
    Enter the source package's name: [quoin]

    Enter the source package's homepage: []
    https://github.com/davidsantiago/quoin
    Enter the year this release is copyrighted: []
    2014
    Enter the upstream author's name: []
    David Santiago
    Enter the upstream author's email: []
    <david.santiago@gmail.com>
    Enter the upstream license, in abbreviated form: [EPL-1.0]

    Enter the names of any dependencies, separated by commas: []

    Enter the package maintainer(s): [Debian Java Maintainers <pkg-java-maintainers@lists.alioth.debian.org>]

    Enter the package uploader(s): [Elana Hashman <debian@hashman.ca>]

    Enter the project description, ending with a blank line:
    Clojure utilities for writing template engines
     Provides map access and text manipulation functions for
     writing template engines in Clojure.

    Generating pom...
    Wrote /export/scratch/ehashman/debian/quoin/pom.xml

    Moving pom to debian/pom.xml...
    Now you can create your Debian changelog with `dch --create`.

    Once you have committed these changes to version control, you may build your package with `gbp gbp buildpackage -uc -us`.
ehashman@corn-syrup:~/debian/quoin$ dch --create
ehashman@corn-syrup:~/debian/quoin$ git add debian/
ehashman@corn-syrup:~/debian/quoin$ git commit -am "Add debian packaging"
    [master 72b7cb2] Add debian packaging
     11 files changed, 340 insertions(+)
     create mode 100644 debian/changelog
     create mode 100644 debian/compat
     create mode 100644 debian/control
     create mode 100644 debian/copyright
     create mode 100644 debian/quoin.classpath
     create mode 100644 debian/quoin.doc-base
     create mode 100644 debian/quoin.docs
     create mode 100644 debian/quoin.jlibs
     create mode 100644 debian/quoin.poms
     create mode 100755 debian/rules
     create mode 100644 debian/source/format
ehashman@corn-syrup:~/debian/quoin$ gbp buildpackage -uc -us
     dpkg-buildpackage -rfakeroot -us -uc -i -I
    dpkg-buildpackage: info: source package quoin-clojure
    dpkg-buildpackage: info: source version 0.1.2-1
    dpkg-buildpackage: info: source distribution unstable
    dpkg-buildpackage: info: source changed by Elana Hashman <debian@hashman.ca>
     dpkg-source -i -I --before-build quoin
    dpkg-buildpackage: info: host architecture amd64
     fakeroot debian/rules clean
    dh clean --with javahelper --with jh_maven_repo_helper
       dh_testdir
       dh_auto_clean
       dh_autoreconf_clean
       debian/rules override_jh_clean
    make[1]: Entering directory '/export/scratch/ehashman/debian/quoin'
    jh_clean
    rm -f /export/scratch/ehashman/debian/quoin/quoin.jar
    make[1]: Leaving directory '/export/scratch/ehashman/debian/quoin'
       mh_clean
       dh_clean
     dpkg-source -i -I -b quoin
    dpkg-source: info: using source format '3.0 (quilt)'
    dpkg-source: info: building quoin-clojure using existing ./quoin-clojure_0.1.2.orig.tar.gz
    dpkg-source: info: building quoin-clojure in quoin-clojure_0.1.2-1.debian.tar.xz
    dpkg-source: info: building quoin-clojure in quoin-clojure_0.1.2-1.dsc
     debian/rules build
    dh build --with javahelper --with jh_maven_repo_helper
       dh_testdir
       dh_update_autotools_config
       dh_autoreconf
       dh_auto_configure
       jh_linkjars
       dh_auto_build
       debian/rules override_jh_build
    make[1]: Entering directory '/export/scratch/ehashman/debian/quoin'
    jar cf quoin.jar -C src .
    make[1]: Leaving directory '/export/scratch/ehashman/debian/quoin'
       dh_auto_test
       create-stamp debian/debhelper-build-stamp
     fakeroot debian/rules binary
    dh binary --with javahelper --with jh_maven_repo_helper
       dh_testroot
       dh_prep
       dh_auto_install
       jh_installjavadoc
       dh_installdocs
       dh_installchangelogs
       dh_perl
       dh_link
       debian/rules override_jh_installlibs
    make[1]: Entering directory '/export/scratch/ehashman/debian/quoin'
    jh_installlibs quoin.jar
    make[1]: Leaving directory '/export/scratch/ehashman/debian/quoin'
       debian/rules override_jh_classpath
    make[1]: Entering directory '/export/scratch/ehashman/debian/quoin'
    jh_classpath quoin.jar
    make[1]: Leaving directory '/export/scratch/ehashman/debian/quoin'
       jh_manifest
       jh_exec
       jh_depends
       mh_installpoms
       mh_linkjars --skip-clean-poms
       dh_strip_nondeterminism
       dh_compress
       dh_fixperms
       dh_installdeb
       dh_gencontrol
       dh_md5sums
       dh_builddeb
    dpkg-deb: building package 'libquoin-clojure' in '../libquoin-clojure_0.1.2-1_all.deb'.
     dpkg-genbuildinfo
     dpkg-genchanges  >../quoin-clojure_0.1.2-1_amd64.changes
    dpkg-genchanges: info: including full source code in upload
     dpkg-source -i -I --after-build quoin
    dpkg-buildpackage: info: full upload (original source is included)
    Now running lintian...
    Finished running lintian.
```

## License

Copyright © 2017 Elana Hashman

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
