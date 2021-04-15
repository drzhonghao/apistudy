# Should we extract API usage examples from client or library? : An empirical study

## Project summary


Programmers often learn APIs from code samples. The state-of-the-art code search engines are built upon huge repositories, and can retrieve many clients, given an API name as its input. In the literature, researchers have conduct various empirical studies on API usages, and as an important source, all the studies analyzed API usages in clients. Although these studies present useful findings, their conclusions are partial, because there are other sources for API usages. For example, libray code itself can contain many API usages. The prior studies do not analyze library code, because it contains internal usages. If researchers cannot remove such internal usages, their conclusions will be polluted. As a result, many questions on API usages are still partially answered and the questions on API usages inside libraries are all open. For example, how many API usages are there inside libraries? What are the differences of API usages between client code and library code? The answers to these questions are useful in various applications. For example, when programmers search for less popular APIs or new APIs, they often cannot retrieve useful samples from code search engines. Our answers are useful to find more API usages. 

To support our study, we implement a tool to incrementally and systematically remove internal usages. With its support, we conduct the first empirical study on API usages inside libraries. Taking a popular code search (SearchCode) as a baseline, we compare API usages in client code with those in library code from various perspectives. 


## Samples from client code and library code

In our comparison, we selected five libraries: 
[accumulo](https://accumulo.apache.org), [cassandra](https://cassandra.apache.org), [karaf](https://karaf.apache.org), [lucene](https://lucene.apache.org), and [poi](https://poi.apache.org).

The API documents of the above libraries are hosted: [accumulo](https://tinyurl.com/wrxtcag), [cassandra](https://tinyurl.com/s48gayr), [karaf](https://tinyurl.com/yb23bygh), [lucene](https://tinyurl.com/r65nw7q), and [poi](https://tinyurl.com/uzgdlyr).

### Client code
We used [SearchCode](https://searchcode.com) and our tool to retrieve code samples for the API classes which are defined in the above API documents. 
We require that each synthesized code sample of our tool shall have no compilation errors. We require that each code sample of SearchCode shall call the latest APIs that are defined in the above documents. 

### Library code
The samples from our tool are under their corresponding folders. Please note that although each file has no compilation errors, compiling all of them together can have compilation errors. These errors are not caused by internal usages, but the ambigious code names across files. These errors shall not pose any barriers to learn API usages, in that each file has no compilation errors. 


## The programming tasks and our comparison results

We constructed 20 programming tasks, and prepared their gold standard. We invite four students to attack the tasks under two treatments: using SearchCode and using CodeSyner. 

The empty tasks, the golden standards, and all the results of the students can be downloaded from [figshare](https://figshare.com/s/085b5ad9c6ac930b45ce).




