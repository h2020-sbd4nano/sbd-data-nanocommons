// Copyright (c) 2022-2023  Egon Willighagen <egon.willighagen@gmail.com>
//
// GPL v3

@Grab(group='io.github.egonw.bacting', module='managers-rdf', version='0.3.3')
@Grab(group='io.github.egonw.bacting', module='managers-ui', version='0.3.3')
@Grab(group='io.github.egonw.bacting', module='net.bioclipse.managers.jsoup', version='0.3.3')

bioclipse = new net.bioclipse.managers.BioclipseManager(".");
rdf = new net.bioclipse.managers.RDFManager(".");
jsoup = new net.bioclipse.managers.JSoupManager(".");

root = "https://ssl.biomax.de/nanocommons/"

htmlContent = bioclipse.download("${root}cgi/login_bioxm_portal.cgi")

htmlDom = jsoup.parseString(htmlContent)

// application/ld+json

bioschemasSections = jsoup.select(htmlDom, "script[type='application/ld+json']");

links = []
for (section in bioschemasSections) {
  links.add(section.attr("src").replace("../", root))
}

kg = rdf.createInMemoryStore()

for (link in links) {
  bioschemasJSON = bioclipse.download(link)
  rdf.importFromString(kg, bioschemasJSON, "JSON-LD")
}

turtle = rdf.asTurtle(kg);

println "#" + rdf.size(kg) + " triples detected in the JSON-LD"
//println turtle

sparql = """
PREFIX schema: <http://schema.org/>
SELECT ?dataset ?url ?name ?license ?description WHERE {
  ?dataset a schema:Dataset ;
    schema:url ?url .
  OPTIONAL { ?dataset schema:name ?name }
  OPTIONAL { ?dataset schema:license ?license }
  OPTIONAL { ?dataset schema:description ?description }
} ORDER BY ASC(?dataset)
"""

results = rdf.sparql(kg, sparql)

println "@prefix dc:    <http://purl.org/dc/elements/1.1/> ."
println "@prefix dct:   <http://purl.org/dc/terms/> ."
println "@prefix foaf:  <http://xmlns.com/foaf/0.1/> ."
println "@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> ."
println "@prefix sbd:   <https://www.sbd4nano.eu/rdf/#> ."
println "@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> ."
println "@prefix void:    <http://rdfs.org/ns/void#> ."
println ""
println "<https://ssl.biomax.de/nanocommons/cgi/login_bioxm_portal.cgi>"
println " a                    void:DatasetDescription ;"
println " dc:source            <https://ssl.biomax.de/nanocommons/> ;"
println " dct:title            \"NanoCommons Knowledge Base - powered by BioXM™\"@en ;"
println " foaf:img             <https://ssl.biomax.de/nanocommons/pic/biomax_logo_175px.png> ."
println ""

for (i=1;i<=results.rowCount;i++) {
  println "<${results.get(i, "dataset")}> a sbd:Dataset ;"
  println "  dc:source <https://ssl.biomax.de/nanocommons/cgi/login_bioxm_portal.cgi> ;"
  if (results.get(i, "name") != null) println "  rdfs:label \"${results.get(i, "name")}\"@en ;"
  if (results.get(i, "description") != null) println "  dc:description \"${results.get(i, "description")}\"@en ;"
  if (results.get(i, "license") != null) println "  dct:license <${results.get(i, "license")}> ;"
  println "  foaf:page <${results.get(i, "url")}> ."
  println ""
}
