# An Empirical Study on API Usages from Code Search Engine and Local Library

## Project summary


Programmers often learn APIs from code samples. The state-of-the-art code search engines are built upon huge repositories, and can retrieve many clients, given an API name as its input. Although code search engines have Internet-scale repositories, programmers still complain that it is difficult to learn APIs, especially for those new and less popular APIs. As code search engines already have source files from millions of projects, it is less useful to add more projects. It is desirable to explore more sources to learn API usages. 

Library code is often availabel to learn API usages, but libraries contain internal calls. If researchers cannot remove such internal usages, their conclusions will be polluted. As a result, many questions on API usages are still partially answered and the questions on API usages inside libraries are all open. For example, how many API usages are there inside libraries? What are the differences of API usages between client code and library code? The answers to these questions are useful in various applications. For example, when programmers search for less popular APIs or new APIs, they often cannot retrieve useful samples from code search engines. Our answers are useful to find more API usages. 

To support our study, we implement a tool that retrieves Internet code from a code search engine and removes internal calls from library code. With its support, we conduct the first empirical study on API usages inside libraries. In this study, we compare API usages from SearchCode with those in library code from various perspectives. 


## Samples from Internet and local libraries

In our comparison, we selected five libraries: 
[accumulo](https://accumulo.apache.org), [cassandra](https://cassandra.apache.org), [karaf](https://karaf.apache.org), [lucene](https://lucene.apache.org), and [poi](https://poi.apache.org).

The API documents of the above libraries are hosted: [accumulo](https://drzhonghao.github.io/accumulodoc/), [cassandra](https://drzhonghao.github.io/cassandradoc/), [karaf](https://drzhonghao.github.io/karafdoc/), [lucene](https://drzhonghao.github.io/lucenedoc/), and [poi](https://drzhonghao.github.io/poidoc/).

### Internet code search
We used [SearchCode](https://searchcode.com) and our tool to retrieve code samples for the API classes which are defined in the above API documents. 
We require that each synthesized code sample of our tool shall have no compilation errors. We require that each code sample of SearchCode shall call the latest APIs that are defined in the above documents. 

### Local library 
The samples from our tool are under their corresponding folders. Please note that although each file has no compilation errors, compiling all of them together can have compilation errors. These errors are not caused by internal usages, but the ambigious code names across files. These errors shall not pose any barriers to learn API usages, in that each file has no compilation errors. 


## The programming tasks and our comparison results

We constructed 20 programming tasks, and prepared their gold standard. We invite four students to attack the tasks under two treatments: using SearchCode and using CodeSyner. 

The empty tasks, the golden standards, and all the results of the students can be downloaded from [figshare](https://figshare.com/s/085b5ad9c6ac930b45ce).




