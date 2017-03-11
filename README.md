# sureassert-uc

## Introduction

Sureassert UC for Eclipse offers:

* Exemplars – declarative automatic code contract enforcement without test code
* Automatic Continuous Testing
* Real-Time Continuous Test Coverage Reporting
* Declarative Stubbing of Classes, Methods and Individual Code Statements
* Comprehensive JUnit integration

Our driving principal is to provide the tooling required to maximize the benefits of both Contract-First Design and Test Driven Development.  Sureassert UC amalgamates these concepts by treating unit tests as part of the declared contract of every method and running them automatically.  Sureassert Exemplars offer true unit testing capability by isolating the functionality of a single method and enforcing its contract.  The tool also provides integration with JUnit, allowing bespoke (coded) tests to be run within the Sureassert testing context; leveraging the tool’s stubbing, coverage reporting and automated execution capabilities.
Sureassert UC is unique in providing these features with no coding required: no API, configuration files or scripts.  The UC engine generates and executes tests and stubbed code based on the suite of class, method and field-level annotations provided by the tool.  It does this totally automatically by plugging into the Eclipse incremental build process.
Software constructed using Sureassert UC benefits from:

* Increased quality driven by facilitating a more methodical contract-first approach, and enforcement of contracts and coverage thresholds during development, in real-time
* Reduced time to market resulting from less time writing test code, integrated stubbing and reduced overhead of design for testability
* Simplification – no configuration files, APIs or test classes… using Sureassert UC is easy!
* The Sureassert UC Eclipse Engine is available feature-complete for free download and is compatible with Eclipse versions 3.4 and above.
* It is totally free of charge for developing your personal or commercial applications.

## Documentation

Comprehensive documentation is available at http://www.getnewdigital.com/projects/sureassert

## Developing

Sureassert UC is now archived and not under active development.  

You will need Eclipse PDE.  Sureassert UC was last tested with Eclipse Juno.

The source code is packaged in a number of Eclipse plug-ins - you need to import the following into your Eclipse PDE workspace:
com.sureassert.uc  -  the main code for the plugin
com.sureassert.uc.feature  -  required to build an Eclipse feature
com.sureassert.uc.lib  -  container project for dependencies
com.sureassert.uc.runner  --  bootstrap plugin for run sureassert as a run/debug target
com.sureassert.uc.runner.init  -- contains initialization code required for runner to work
com.sureassert.uc.runtime  --  code that is loaded within the project-under-test's classloader at runtime

There is a launch in the root of the com.sureassert.uc project that when run will compile the plugins and run them in a test workspace.  Sureassert UC is an interesting project to test - it cannot be used to test itself as it needs to hide itself from the classloader of the running application (via use of a custom classloader).  The test workspace (runtime-EclipseApplication) contains a number of test projects which can be refreshed to assert all the various features of the product work (effectively a regression suite), and generally tweaked with to test the plugin as it would be used by a user.

To package, open plugin.xml in com.sureassert.uc.feature and com.sureassert.uc and press the Export a Deployable Version button.  That builds the plugins and puts them in com.sureassert.uc.feature/deploy (which its best to clear first).
