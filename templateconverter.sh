#!/bin/bash
# Script for automatically converting csv and template files into triples

OUTPUT=output-triples

if [ ! -d $OUTPUT ]; then
    sudo -u $USER mkdir $OUTPUT 
fi

for csv in $(ls datasets); do java -jar dist/lib/csv2rdf.jar templates/${csv:0:-4}-template.nt datasets/$csv $OUTPUT/${csv:0:-4}-triples.nt; done

# truncate output if "test" passed as first arg 
if [ "$1" == "test" ]; then
    for triple in $(ls $OUTPUT); do truncate --size=10000 $OUTPUT/$triple; done
fi
