# Synthesizing Samples for New and Less Popular APIs

## Project summary


Programmers often refer to code search engines to retrieves samples of APIs. The state-of-the-art code search engines are built upon huge repositories. For example, a code search engine, SearchCode, has a repository of more than 20 billion lines of code. Although these engines are quite useful, when programmers search for less popular APIs or new APIs, they often cannot retrieve useful samples from code search engines. Indeed, our empirical study shows that even if its repository has 20 billion lines of code, SearchCode typically retrieves uptodate samples for only about 10% API classes. 


In this project, instead of searching from more projects, we propose the first approach, called CodeSyner, that synthesizes the samples of an API library from the source files of the library. Comparing with samples from client code, library code can contain internal usages, which introduce compilation errors if such usages appear in client code. The basic idea of CodeSyner is to remove such internal usages incrementally and symmetrically. To achieve this goal, CodeSyner implements a set of operators to modify code, and guides the process with the genetic algorithm. The current implement can synthesize samples from Java libraries.


## Comparison with SearchCode

In our comparison, we selected five libraries: 
[accumulo](https://accumulo.apache.org), [cassandra](https://cassandra.apache.org), [karaf](https://karaf.apache.org), [lucene](https://lucene.apache.org), and [poi](https://poi.apache.org).

The API documents of the above libraries are hosted: [accumulo](https://tinyurl.com/wrxtcag), [cassandra](https://tinyurl.com/s48gayr), [karaf](https://tinyurl.com/yb23bygh), [lucene](https://tinyurl.com/r65nw7q), and [poi](https://tinyurl.com/uzgdlyr).

We used [SearchCode](https://searchcode.com) and our tool to retrieve code samples for the API classes which are defined in the above API documents. 
We require that each synthesized code sample of our tool shall have no compilation errors. We require that each code sample of SearchCode shall call the latest APIs that are defined in the above documents. 

Our synthesized samples are under their corresponding folders. Please note that although each file has no compilation errors, compiling all of them together can have compilation errors. These errors are not caused by internal usages, but the ambigious code names across files. These errors shall not pose any barriers to learn API usages, in that each file has no compilation errors. 


## Assisting programming tasks

We constructed 20 programming tasks, and prepared their gold standard. We invite four students to attack the tasks under two treatments: using SearchCode and using CodeSyner. 

The empty tasks, the golden standards, and all the results of the students can be downloaded from [figshare](https://figshare.com/s/085b5ad9c6ac930b45ce).


## Tool

Due to the consideration of the anonymous review process, we choose to release the source files, after the paper is accepted.  

