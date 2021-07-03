---
title: Apache Jena SPARQL APIs
slug: index
---



#  Apache Jena SPARQL APIs

TOC

## Overview

API =<br/>
GPI =


| API | GPI |
| === | === |
| Model       | Graph |
| Statement   | Triple |
| Resource    | Node |
| Literal     | Node |
| RDFConnection | RDFLink |
| QueryExecution | QueryExec |

    // ** Design
    // + java.net.http
    //     RDFConnection, RDFLink - Builders for remote (local version is simple)
    //     QueryExec and QueryExecution. Builders
    // + GSP engine, Support for quads.
    // + SERVICE rewrite
    // + RowSet - ResultSet for Nodes
    // + Utilities: HttpRDF, AsyncHttpRDF, HttpOp(2)
    // * HttpOp : smaller and mainly in support of HttpRDF which in turn is used by GSP
    // * New HttpRDF (GET/POST/PUT/DELETE graphs and datasets): AsyncHttpRDF (Async GET)

Builders

QueryExecutionBuilder.java
UpdateExecutionBuilder.java

QueryExecutionHTTPBuilder.java
QueryExecBuilder.java
QueryExecHTTPBuilder.java

UpdateExecutionHTTPBuilder.java

ExecBuilderQueryHTTP.java
RDFLinkRemoteBuilder.java

| Utilities | Function |
| ========= | =======  |
| GSP       |          |
| HttpOp    |          |
| HttpRDF   |          |

## `GSP`

## `RDFConnection`

source/documentation/rdfconnection/__index.md

Builders and patterns of use - see TestService.

## QExec(ution)

Builders and patterns of use - see TestService.
--> elsewhere

## Cusomization

Authorization 
Params (e.g. apikey)

## `SERVICE`

/documentation/query/service.md
Request modifier
Params

## Authentication

source/documentation/query/http-auth.md

## @@

source/documentation/query/__index.md
