package data;

import at.ac.tuwien.big.we14.lab4.dbpedia.api.DBPediaService;
import at.ac.tuwien.big.we14.lab4.dbpedia.api.SelectQueryBuilder;
import at.ac.tuwien.big.we14.lab4.dbpedia.vocabulary.DBPedia;
import at.ac.tuwien.big.we14.lab4.dbpedia.vocabulary.DBPediaOWL;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import models.Question;
import play.Logger;

import java.util.List;
import java.util.Locale;

public class DBpedia {

    public static void loadQuestion(Question question, String actorName, String directorName) {

        // Check if DBpedia is available
        if(!DBPediaService.isAvailable()) {
            Logger.info("DBpedia is currently not available.");
            return;
        }

        // Load all statements as we need to get the name later
        Resource director = DBPediaService.loadStatements(DBPedia.createResource(directorName));

        // Load all statements as we need to get the name later
        Resource actor = DBPediaService.loadStatements(DBPedia.createResource(actorName));

        // retrieve english and german names, might be used for question text
        String englishDirectorName = DBPediaService.getResourceName(director, Locale.ENGLISH);
        String germanDirectorName = DBPediaService.getResourceName(director, Locale.GERMAN);
        String englishActorName = DBPediaService.getResourceName(actor, Locale.ENGLISH);
        String germanActorName = DBPediaService.getResourceName(actor, Locale.GERMAN);

        // build SPARQL-query
        SelectQueryBuilder movieQuery = DBPediaService.createQueryBuilder()
                .setLimit(5) // at most five statements
                .addWhereClause(RDF.type, DBPediaOWL.Film)
                .addPredicateExistsClause(FOAF.name)
                .addWhereClause(DBPediaOWL.director, director)
                .addFilterClause(RDFS.label, Locale.GERMAN)
                .addFilterClause(RDFS.label, Locale.ENGLISH);

        // retrieve data from dbpedia
        Model timBurtonMovies = DBPediaService.loadStatements(movieQuery.toQueryString());

        // get english and german movie names, e.g., for right choices
        List<String> englishTimBurtonMovieNames =
                DBPediaService.getResourceNames(timBurtonMovies, Locale.ENGLISH);
        List<String> germanTimBurtonMovieNames =
                DBPediaService.getResourceNames(timBurtonMovies, Locale.GERMAN);

        // alter query to get movies without tim burton
        movieQuery.removeWhereClause(DBPediaOWL.director,director);
        movieQuery.addMinusClause(DBPediaOWL.director,director);

        // retrieve data from dbpedia
        Model noTimBurtonMovies = DBPediaService.loadStatements(movieQuery.toQueryString());

        // get english and german movie names, e.g., for wrong choices
        List<String> englishNoTimBurtonMovieNames =
                DBPediaService.getResourceNames(noTimBurtonMovies, Locale.ENGLISH);
        List<String> germanNoTimBurtonMovieNames =
                DBPediaService.getResourceNames(noTimBurtonMovies, Locale.GERMAN);
    }
}
