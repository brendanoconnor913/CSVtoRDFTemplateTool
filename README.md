# CSVtoRDFTemplateTool
Builds on [CSV2RDF](https://github.com/clarkparsia/csv2rdf) to create templates for conversion
## Overview   
This tool was built with the intention of making it easy to convert csv files from separate organizations 
(i.e. governments, ngo's, etc.) into a common RDF format.
In these scenarios the "subject" of the csv documents is usually expressable by a subset of the the columns
(typically description columns).
This tool gets input from the user (names of entities and subject columns) and creates triples and quads (context given by user).
The end result is a tool for easily converting csv files to nt triples.   

## Usage  
The program takes three arguments when running 1) the csv or directory containing all of the csv files 2) the ontology graph 
3) the template graph. The ontology graph will be searched to try and identify entities from the column header strings. If it is 
not found the user will be asked to provide the name of the entity. The template graph works in a similar manner if it can identify
a previously defined template from the entities it will use it and if not it will ask the user to define the subject entities.   
   
## Improvements   
The template graph currently doesn't update the number of observations or times it has been used. This needs to be fixed so
the tool can be extended to use a simple conditional probability to attempt to guess the subject entities to be used.
Ideally it would use the column header entities to search previous templates (prefaced by the number of entities in common)
to determine the probability of each column being in the subject. Then an algorithm similar to squential forward feature selection
could be used to construct the subject by one by one adding the most probable subject. After each subject entity is added the
expected subject size (calculated from previously obtained templates) could be calculated. This process would countinue until the
addition of another entity would exceed the expected subject size. Don't think I'll ever get around to implementing but outlining
in case someone else may want to. 
