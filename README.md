# Synthesizing Samples for New and Less Popular API Classes

## Project summary


Programmers often refer to code search engines to retrieves samples of APIs, and the state-of-the-art code search engine can retrieve samples from a repository of 20 billion lines of code. Although they are quite useful, when programmers search for less popular APIs or new APIs, they often find that existing code search engines cannot retrieve samples. Indeed, our empirical study shows that even if its repository has 20 billion lines of code, the state-of-the-art code search engine typically retrieves uptodate samples for only about 10% API classes. In this project, instead of searching from existing projects, we propose the first approach, called CodeSyner, that synthesizes samples from API code. Comparing with samples from client code, API code can contain internal usages, which introduce compilation errors if such usages appear in client code. The basic idea of CodeSyner is to remove such internal usages incrementally and symmetrically. To achieve this goal, CodeSyner implement a set of operators to modify code, and guides the process with the genetic algorithm. The current implement can synthesize samples from API code in Java.


## Comparison

In our comparison, we selected five libraries: 
[accumulo](https://accumulo.apache.org), [cassandra](https://cassandra.apache.org), [karaf](https://karaf.apache.org), [lucene](https://lucene.apache.org), and [poi](https://poi.apache.org).

The API documents of the above libraries are hosted: [accumulo](https://tinyurl.com/wrxtcag), [cassandra](https://tinyurl.com/s48gayr), [karaf](https://tinyurl.com/yb23bygh), [lucene](https://tinyurl.com/r65nw7q), and [poi](https://tinyurl.com/uzgdlyr).

We used [SearchCode](https://searchcode.com) and our tool to retrieve code samples for the API classes which are defined in the above API documents. 
We require that each synthesized code sample of our tool shall have no compilation errors. We require that each code sample of SearchCode shall call the latest APIs that are defined in the above documents. 

Our synthesized samples are under their corresponding folders. Please note that although each file has no compilation errors, compiling all of them together can have compilation errors. These errors are not caused by internal usages, but the ambigious code names across files. These errors shall not pose any barriers to learn API usages, in that each file has no compilation errors. 


## Comparison

We constructed 20 [programming tasks](https://github.com/tohidemyname/newapi/blob/master/task.poi.rar). The gold standard of the tasks is listed [here](https://github.com/tohidemyname/newapi/blob/master/task.poi.gold.rar). 

The results of the study are listed in [1](https://github.com/tohidemyname/newapi/blob/master/task.poi.1.rar), [2](https://github.com/tohidemyname/newapi/blob/master/task.poi.2.rar), [3](https://github.com/tohidemyname/newapi/blob/master/task.poi.3.rar), and [4](https://github.com/tohidemyname/newapi/blob/master/task.poi.4.rar).

As github does not allow to update a file whose size is too large, we have to remove the depenencies under the lib directary. 


## Tool

Due to the consideration of the anonymous review process, we choose to release the source files, after the paper is accepted.  

