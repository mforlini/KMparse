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
This information can then be used to refactor class names, function names, and properties that have been stripped out by R8 or ProGuard. While the metadata will not be obfuscated by R8/ProGuard, whether it is removed depends on the particulars of the app.

## Example
An example of a simple obfuscated class from an Android app and KMparse output from the class's smali file.
#### Example class decompiled with [jadx](https://github.com/skylot/jadx)
```java
public final class a extends b {
    private final String c;

    public a(Context context, String str, String str2) {
      //...
    }

    public static int a(int i, int i2) {
      //...
    }

    public final void a() {
      //...
    }
}
```
#### KMparse output on a.smali
```
Type: Class
Class Info:
    Name: io/github/mforlini/KMparseExample/HelloWorld
    Supertypes: Class(name=io/github/mforlini/KMparseExample/OurMessenger)
    Module Name: app_release
    Type Aliases: 
    Companion Object: 
    Nested Classes:  
    Enum Entries: 

Constructors:
    <init>(Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;)V, Arguments: context, greeting, message

Functions:
    computeSecret(II)I, Arguments: lowerBound, upperBound
    showMessage()V, Arguments: 

Properties:
    greeting:Ljava/lang/String;
```
## Installation

## Usage
```
usage: kmparse [-h] [-a] [-f] SOURCE [DEST]


Parses Kotlin metadata annotations from smali files into human readable class
information


optional arguments:
  -h,       show this help message and exit
  --help

  -a,       force parsing of all files even when input contains a smali
  --all     directory

  -f,       delete destination directory
  --force


positional arguments:
  SOURCE    source filename

  DEST      destination directory
  ```
