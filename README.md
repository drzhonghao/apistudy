# Updating Client Code to Call More Recent Libraries without Examples

## Project summary


Programmers often refer to code search engines to retrieves samples of APIs, and the state-of-the-art code search engine can retrieve samples from a repository of 20 billion lines of code. Although they are quite useful, when programmers search for less popular APIs or new APIs, they often find that existing code search engines cannot retrieve samples. Indeed, our empirical study shows that even if its repository has 20 billion lines of code, the state-of-the-art code search engine typically retrieves uptodate samples for only about 10% API classes. In this project, instead of searching from existing projects, we propose the first approach, called CodeSyner, that synthesizes samples from API code. Comparing with samples from client code, API code can contain internal usages, which introduce compilation errors if such usages appear in client code. The basic idea of CodeSyner is to remove such internal usages incrementally and symmetrically. To achieve this goal, CodeSyner implement a set of operators to modify code, and guides the process with the genetic algorithm. The current implement can synthesize samples from API code in Java.


## Evaluation



## Tool

Due to the consideration of the anonymous review process, we choose to release the source files, after the paper is accepted.  

