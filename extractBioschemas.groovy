// Copyright (c) 2022-2024  Egon Willighagen <egon.willighagen@gmail.com>
//
// GPL v3

@Grab(group='io.github.egonw.bacting', module='managers-rdf', version='0.5.0')
@Grab(group='io.github.egonw.bacting', module='managers-ui', version='0.5.0')
@Grab(group='io.github.egonw.bacting', module='net.bioclipse.managers.jsoup', version='0.5.0')

bioclipse = new net.bioclipse.managers.BioclipseManager(".");
rdf = new net.bioclipse.managers.RDFManager(".");
jsoup = new net.bioclipse.managers.JSoupManager(".");

htmlContent = bioclipse.download("https://nanocommons.github.io/datasets/")

htmlDom = jsoup.parseString(htmlContent)

// application/ld+json

bioschemasSections = jsoup.select(htmlDom, "script[type='application/ld+json']");

kg = rdf.createInMemoryStore()

for (section in bioschemasSections) {
  bioschemasJSON = section.html()
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

store = rdf.createInMemoryStore(false)
rdf.addPrefix(store, "dc", "http://purl.org/dc/elements/1.1/")
rdf.addPrefix(store, "dct", "http://purl.org/dc/terms/")
rdf.addPrefix(store, "foaf", "http://xmlns.com/foaf/0.1/")
rdf.addPrefix(store, "rdfs", "http://www.w3.org/2000/01/rdf-schema#")
rdf.addPrefix(store, "sbd", "https://www.sbd4nano.eu/rdf/#")
rdf.addPrefix(store, "xsd", "http://www.w3.org/2001/XMLSchema#")
rdf.addPrefix(store, "void", "http://rdfs.org/ns/void#")

propType    = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"
propSource  = "http://purl.org/dc/elements/1.1/source"
propLicense = "http://purl.org/dc/terms/license"

metaIRI = "https://nanocommons.github.io/datasets/"
rdf.addObjectProperty(store, metaIRI, propType, "http://rdfs.org/ns/void#DatasetDescription")
rdf.addObjectProperty(store, metaIRI, propSource, "https://nanocommons.github.io/datasets/")
rdf.addPropertyInLanguage(store, metaIRI, "http://purl.org/dc/terms/title", "Overview of open datasets released by NanoSafety Cluster projects", "en")
rdf.addObjectProperty(store, metaIRI, "http://xmlns.com/foaf/0.1/img", "https://upload.wikimedia.org/wikipedia/commons/e/e1/NanoCommons-Logo-Large_-_White_Circle_01.png")
rdf.addObjectProperty(store, metaIRI, propLicense, "http://creativecommons.org/publicdomain/zero/1.0/")

for (i=1;i<=results.rowCount;i++) {
  datasetIRI = results.get(i, "dataset")
  rdf.addObjectProperty(store, datasetIRI, propType, "https://www.sbd4nano.eu/rdf/#Dataset")
  rdf.addObjectProperty(store, datasetIRI, propSource, metaIRI)
  if (results.get(i, "name") != null)
    rdf.addPropertyInLanguage(store, datasetIRI,
      "http://www.w3.org/2000/01/rdf-schema#label", results.get(i, "name"), "en")
  if (results.get(i, "description") != null)
    rdf.addPropertyInLanguage(store, datasetIRI,
      "http://purl.org/dc/elements/1.1/description", results.get(i, "description"), "en")
  if (results.get(i, "license") != null) rdf.addObjectProperty(store, datasetIRI, propLicense, results.get(i, "license"))
  rdf.addObjectProperty(store, datasetIRI, "http://xmlns.com/foaf/0.1/page", results.get(i, "url"))
  println ""
}

println rdf.asTurtle(store)
