Facsimile
=========

A facsimile viewer implemented as a [reagent](https://github.com/reagent-project/reagent) component utilising [custom HTML components](https://developer.mozilla.org/en-US/docs/Web/Web_Components/Using_custom_elements) to embed [TEI-flavoured XML](https://tei-c.org/) directly into an HTML page.


Development prerequisites
-------------------------
The development workflow is built around the [Clojure CLI](https://clojure.org/reference/deps_and_cli) for managing dependencies and [shadow-cljs](https://github.com/thheller/shadow-cljs) for compiling ClojureScript code and providing a live-reloading development environment.

In this project, the dependency management feature of shadow-cljs is not used directly. Rather, we leverage the built-in support in shadow-cljs for the Clojure CLI/deps.edn to download dependencies and build a classpath.

I personally use IntelliJ with the Cursive plugin which [integrates quite well with the Clojure CLI](https://cursive-ide.com/userguide/deps.html).

### macOS setup
(assuming [homebrew](https://brew.sh/) has already been installed)


I'm not sure which JDK version you need, but anything 8+ is probably fine! I personally just use the latest from AdoptOpenJDK (currently JDK 13):

```
brew cask install adoptopenjdk
```

The following will get you the Clojure CLI and shadow-cljs, along with NodeJS:

```
brew install clojure
brew install node
npm install -g shadow-cljs
```

Workflow
--------
Development of the component is done using the live-reloading capabilities of shadow-cljs:

```
shadow-cljs watch app
```

This will start a basic web server serving the `:app` build as specified in the `shadow-cljs.edn` file.

I use the Clojure CLI integration in Cursive to calculate a classpath (clicking `refresh` when I'm reminded to after editing the `deps.edn` file), but I'm told something like this command is being executed behind the scenes:

```
clj -A:shadow-cljs -Spath
```

I have also set up some aliases in my personal [~/.clojure/deps.edn](https://github.com/simongray/dotfiles/blob/master/dot/clojure/deps.edn) file to perform certain common tasks such as listing/updating outdated packages:

```
clj -A:outdated
clj -A:update
```
