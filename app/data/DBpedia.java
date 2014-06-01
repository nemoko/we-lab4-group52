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
import models.Category;
import models.Choice;
import models.Question;
import play.Logger;

import java.math.BigDecimal;
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

        question.setTextDE("In welchen Filmen hat " + germanActorName + " gespielt und " + germanDirectorName + " Regie geführt");
        question.setTextEN("In which movies acted " + englishActorName + " and " + englishDirectorName + " directed");
        question.setMaxTime(new BigDecimal(45));

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
        List<String> englishTimBurtonMovieNames = DBPediaService.getResourceNames(timBurtonMovies, Locale.ENGLISH);
        List<String> germanTimBurtonMovieNames = DBPediaService.getResourceNames(timBurtonMovies, Locale.GERMAN);

        for(String movie : englishTimBurtonMovieNames) {
            Choice correctChoice = new Choice();
            correctChoice.setTextEN(movie);
            question.addRightChoice(correctChoice);
        }

        for(String movie : germanTimBurtonMovieNames) {
            Choice correctChoice = new Choice();
            correctChoice.setTextDE(movie);
            question.addRightChoice(correctChoice);
        }
    }
}
