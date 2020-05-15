rescope
=======
By combining modern browser features such as the [shadow DOM](https://developer.mozilla.org/en-US/docs/Web/Web_Components/Using_shadow_DOM) and [custom HTML elements](https://developer.mozilla.org/en-US/docs/Web/Web_Components/Using_custom_elements), `rescope` lets you embed most documents directly in a [re-frame](https://github.com/day8/re-frame) or [reagent](https://github.com/reagent-project/reagent) application. Rescoping requires very little code, mostly just CSS. At the same time, escape hatches are provided for when you need to restructure or add interactivity to your documents.

Through [rescope.formats.xml](https://github.com/kuhumcst/rescope/tree/master/src/kuhumcst/rescope/formats), you can embed XML directly in your web page as if it were completely valid HTML. With this new property, XML is no longer just for data exchange, but can be viewed as a visualisation of a document, a widget, or maybe even a little application.

CSS is automatically patched to reflect the tag & attribute renaming that allows the XML to moonlight as HTML. This means regular CSS can be written directly for the XML. And since both CSS and XML are scoped inside a shadow DOM, there is no chance of clashing with any other CSS on the page.

However, `rescope` can embed, style, and patch _any_ [hiccup-like](https://github.com/weavejester/hiccup) structure -- quite handy in the ClojureScript world! For example, the default output of [instaparse](https://github.com/Engelberg/instaparse) is a hiccup-like structure. The implication is clear: anything that can be parsed can potentially be rescoped as an interactive reagent component.

Quickstart
----------
Building a `rescope` component from scratch is quite simple:

1. Find a way to `parse` your chosen document type as hiccup. For XML, you can simply use the included parser.
2. Write some CSS for visual structure and patch it using `prefix-css`.
3. _(optional)_ Create an `injector` function to inject reagent components at appropriate points.
4. Postprocess the hiccup using the `postprocess` function.
5. Display the hiccup with scoped CSS using the `scope` component.

### Frontend or backend?
Parsing can take place in both the frontend and the backend. For performance reasons it can sometimes make sense to perform this step in the backend -- or at least memoize it in the frontend. However, doing parsing in the frontend undoubtedly simplifies things and it might even make *more* sense when parsing XML.

Postprocessing, however, should nearly *always* take place in the frontend. The only exception to this is when you serve completely static content, i.e. no injections. Injections are ClojureScript functions and can't be sent from the backend to the frontend. 

### Example

TODO: write a small example.

Origin
------
The original purpose was to be able to display [TEI-flavoured XML](https://tei-c.org/) next to a PDF/image viewer while preserving the XML structure entirely. This has -- so far -- mostly only been possible to do in browsers when serving an entire page's worth of relatively static XML content, although there is some prior art in the form of [CETEIcean](https://github.com/TEIC/CETEIcean) which also uses custom elements. This project, however, seeks to be more flexible in its implementation, while integrating into a reactive UI paradigm and the rich [ClojureScript](https://clojurescript.org/) ecosystem.

Development prerequisites
-------------------------
_NOTE: this part is only relevant if you're developing the rescope library itself._

The development workflow of the project itself is built around the [Clojure CLI](https://clojure.org/reference/deps_and_cli) for managing dependencies and [shadow-cljs](https://github.com/thheller/shadow-cljs) for compiling ClojureScript code and providing a live-reloading development environment.

In this project, the dependency management feature of shadow-cljs is not used directly. Rather, I leverage the built-in support in shadow-cljs for the Clojure CLI/deps.edn to download dependencies and build a classpath.

I personally use IntelliJ with the Cursive plugin which [integrates quite well with the Clojure CLI](https://cursive-ide.com/userguide/deps.html).

### macOS setup
(assuming [homebrew](https://brew.sh/) has already been installed)


I'm not sure which JDK version you need, but anything 8+ is probably fine! I personally just use the latest from AdoptOpenJDK (currently JDK 13):

```
brew cask install adoptopenjdk
```

The following will get you the Clojure CLI and shadow-cljs, along with NodeJS:

```
brew install clojure/tools/clojure
brew install node
npm install -g shadow-cljs
```

Make sure that shadow-cljs can infer and install react-dom:
```
npm init
```

Workflow
--------
Development of the component is done using the live-reloading capabilities of shadow-cljs:

```
shadow-cljs watch app
```

This will start a basic web server at `localhost:8000` serving the `:app` build as specified in the `shadow-cljs.edn` file.

It's possible to execute unit tests while developing by also specifying the `:test` build:

```
shadow-cljs watch app test
```

This will make test output available at `localhost:8100`. It's quite convenient to keep a separate browser tab open just for this. The favicon will be coloured green or red depending on the state of the assertions.

Personally, I use the Clojure CLI integration in Cursive to calculate a classpath and download dependencies. Something like this command is being executed behind the scenes:

```
clj -A:app:test -Spath
```

I have also set up some aliases in my personal [~/.clojure/deps.edn](https://github.com/simongray/dotfiles/blob/master/dot/clojure/deps.edn) file to perform certain common tasks such as listing/updating outdated packages:

```
clj -A:outdated
clj -A:update
```
