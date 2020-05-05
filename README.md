# KMparse

## Introduction

The purpose of KMparse is to deobfuscate Android applications written in Kotlin by way of Kotlin metadata annotations.

#### What is Kotlin metadata

Kotlin metadata is added to all code produced by the Kotlin compiler and its purpose is to retain information on Kotlin language features after the code has been complied into Java.
It is added in the form of an annotation in the complied code and contains all the knowledge that the Kotlin compiler had about the class.
This knowledge includes class names, function signatures and properties.

#### What does KMparse do

Most of the data of a Kotlin metadata annotation is stored in the form of a protocol buffer alongside plain text strings.
KMparse will scan smali files for Kotlin metadata and parse these protocol buffers and strings into a human readable form.
This information can then be used to refactor class names, function names, and properties that have been stripped out by R8 or ProGuard.

## Example

## Installation

## Usage
