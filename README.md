## Eclipse Communication Framework
ECF is a set of APIs/frameworks/types for inter-process communication

Current version: 3.16.2
Release Date: 5/28/2025

## NEW (5/19/2025) ECF adds tooling support for Model Context Protocol (MCP)

As part of ECF 3.16.1, there is a new bundle/jar in ECF that adds annotation classes and a few utility classes.  It's available in [ECF 3.16.1 p2 repo](https://download.eclipse.org/rt/ecf/latest/) and via [maven central](https://central.sonatype.com/artifact/org.eclipse.ecf/org.eclipse.ecf.ai.mcp.tools).

There is an example [here](https://github.com/ECF/Py4j-RemoteServicesProvider/blob/master/examples/org.eclipse.ecf.examples.ai.mcp.toolservice/src/org/eclipse/ecf/examples/ai/mcp/toolservice/ArithmeticTools.java) of using these annotations for two tools declared by a single service interface.

### Background ###

The [Model Context Protocol]() is a rapidly-being-adopted client->server protocol for integrating existing code and services in LLMs, so they can be called by the LLM (e.g. [Anthropic's Claude](https://claude.ai/new)).  One integration approach is by adding Tools to MCP servers.  

A tool for MCP is essentially a single method (in Java) or a function (in Python). When creating new tools for Python servers, usually a decorator defined in the Python SDK would be used. For example:
```
@mcp.tool(description='myfunct does some amazing stuff')
def myfunc() {
   pass
}
```
The MCP Java SDK, however does not yet have equivalent annotation classes (e.g. @Tool, @ToolParam, @ToolAnnotation, etc).   ECF's [new bundle project](https://github.com/eclipse-ecf/ecf/tree/master/framework/bundles/org.eclipse.ecf.ai.mcp.tools), provides these annotation classes in Java.  Although this bundle has *no* external dependencies other than Java 17 (not on other ECF classes nor any other framework), it would be better if these or some annotations were part of the MCP itself, and so we are in the process of [getting annotation classes added to the MCP Java SDK itself](https://github.com/modelcontextprotocol/java-sdk/pull/235) through [this pull request](https://github.com/modelcontextprotocol/java-sdk/pull/235).  If the annotation classes are standardized and in the Java SDK itself in future MCP releases, the references in all ECF code will be updated to depend upon the sdk-provided versions.

## NEW (4/28/2025) Bndtools Template for Python.Java Remote Services Development

There has been a new project template added to the [ECF Bndtools Workspace Template](https://github.com/ECF/bndtools.workspace) that uses the [ECF Python.Java Distribution Provider](https://github.com/ECF/Py4j-RemoteServicesProvider).  This distribution provider is based upon py4j, which supports high performance remote procedure call between python and java processes.

To try it out after installing Bndtools 7.1 and the ECF tools add ons

1. Create a new Bndtools Workspace using the [ECF Bndtools Workspace Template](https://github.com/ECF/bndtools.workspace)

![bndtoolsnewwkspace](https://github.com/user-attachments/assets/95ec5792-6bc2-4c88-990d-4e8d3350627e)

2. Create a new Bnd OSGi project

![bndtoolsnewproject](https://github.com/user-attachments/assets/fa2641e6-a074-4796-b761-f79999b9ba06)

3. Open the projectName.hellopython.javahost.bndrun file in the project directory
   
![bndtoolsbndrun](https://github.com/user-attachments/assets/9bf8a380-9ee7-4e48-ac49-1627cf3ace75)

4. Choose 'Resolve' and then 'Update'

5. Select Debug OSGi to start the example application (Java)

![bndtoolsdebug](https://github.com/user-attachments/assets/9fa2536f-9748-4f5f-94bc-b78374f436a8)

Running Python Example Program 

1. Install [iPOPO v 3.1.0](https://ipopo.readthedocs.io) in your Python (3.9 or greater) local environment

2. In a command shell or IDE, navigate to the project directory and run the run_python_example.py script

```
python run_python_example.py
```
The examples will output progress to their respective consoles as the remote services are made exported,
discovered, and imported by the java process or the python process.  

![bndtoolspython](https://github.com/user-attachments/assets/d5bbd4e4-d57c-412a-a198-fe16ed76a95d)

Most of the code that produces output is available in the example project. For java: src/main/java/.../hello/*.java 
and python: python-src/samples/rsa
   
### Install into Bndtools for Remote Services development
NEW: Feature for Remote Services tooling that enhances [Bndtools](https://bndtools.org/) 7.1 or higher.  Theses tools use bndtools project, workspace, service templates, and OSGi services wizards for simplifying the creation of OSGi remote services.  Also present are Eclipse view for debugging remote service endpoint description discovery and remote service export/import.  The feature requires that Bndtools 7.1+ be [installed](https://bndtools.org/installation.html) into a recent version of Eclipse.  Also see the Install into Bndtools 7.1 via Oomph section below for automated install.

<b>Name</b>:  ECF 3.15.7

<b>Update Site URL</b>:  [https://download.eclipse.org/rt/ecf/latest/site.p2](https://download.eclipse.org/rt/ecf/latest/site.p2)

[Javadocs](https://download.eclipse.org/rt/ecf/latest/javadoc/)

### Install into [Bndtools 7.1](https://bndtools.org/) via Oomph

There are now [Oomph](https://projects.eclipse.org/projects/tools.oomph) Setups that will automatically install and configure Eclipse, Bndtools 7.1 and ECF 3.15.7 [see here](https://github.com/bndtools/bndtools.p2.repo/tree/master/setup/ecf)

### Features for Bndtools-based [OSGi Remote Services](https://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.remoteservices.html) Development

#### Workspace, Project, and Bndrun Templates

Bndtools Workspace Template for Remote Services Development here:  [https://github.com/ECF/bndtools.workspace](https://github.com/ECF/bndtools.workspace).  This workspace template contains multiple project templates for creating Remote Service API, Impl, and Consumer projects via the New->Bnd OSGi Project wizard:

![image](https://github.com/user-attachments/assets/1c775de3-4970-4202-865f-1ac3ba0b0f32)

#### Wizards for creating api, impl, consumer Remote Services projects via a single wizard

![image](https://github.com/user-attachments/assets/674fb4ba-8f67-42fb-8664-341d45fce17a)

#### Eclipse Views for Endpoint Discovery and [Remote Service Admin Manager](https://docs.osgi.org/specification/osgi.cmpn/8.0.0/service.remoteserviceadmin.html)

![image](https://github.com/user-attachments/assets/acd0e785-06db-4136-9b97-9a0ea944a062)

#### Wizard and Template-Created Bndrun files, to immediately run/debug wizard-generated remote services

![image](https://github.com/user-attachments/assets/97f85c7f-78e6-4016-ac8c-bbe014bd9446)

### Download/Install into [Apache Karaf 4.4+](https://karaf.apache.org/)

For Install into Karaf runtime, [here is a top-level Karaf Features file](https://download.eclipse.org/rt/ecf/latest/karaf-features.xml)

## Key ECF APIs

### OSGi Remote Services
ECF provides a fully-compliant and multi-provider implementation of the [OSGi Remote Services](https://docs.osgi.org/specification/osgi.cmpn/8.0.0/service.remoteservices.html) and [Remote Services Admin/RSA](https://docs.osgi.org/specification/osgi.cmpn/8.0.0/service.remoteserviceadmin.html).  A number of providers are available in this repo, but there are also many providers available at the [ECF github organization](https://github.com/ECF).

ECF is the OSGi R8 RS/RSA implementation in the [OSGi Test Compatibilty Kit (TCK)](https://github.com/osgi/osgi)

### Eclipse Install/Update File Transfer
ECF filetransfer is used by the [Eclipse IDE](https://github.com/eclipse-platform)

## ECF Github Organization
ECF  has an [organization with a number of other repos](https://github.com/ECF) containing Remote Services distribution and discovery providers (e.g. grpc, etcd discovery, hazelcast, JMS, JGroups, xmlrpc-based distribution providers, examples, others). Most of these repos provide distribution or discovery providers that depend upon the core remote services/RSA implementation provided by this repo.  

## Wiki
See the [ECF Wiki](https://wiki.eclipse.org/Eclipse_Communication_Framework_Project) for examples, tutorials, Karaf install documentation, other documentation.

To contribute or find out what's going on right now, please join the [ecf-dev mailing list](https://accounts.eclipse.org/mailing-list/ecf-dev) or contact project lead Scott Lewis at github email: scottslewis at gmail.com

## Services, Training,  and Support
For Remote Services training, support, or custom OSGi or Eclipse development please contact scottslewis at gmail.com via email or post on the [ecf-dev mailing list](https://accounts.eclipse.org/mailing-list/ecf-dev)  

### Contributing to ECF
Contributions are always welcome!  For 20 years ECF has been innovating via community contributions.
See [CONTRIBUTING.md](CONTRIBUTING.md)
