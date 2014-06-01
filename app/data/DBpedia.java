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
import models.QuizDAO;
import play.Logger;
import play.db.jpa.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DBpedia {

    @Transactional
    public static void insertData() {

        Category category = new Category();
        loadCategory(category);

        QuizDAO.INSTANCE.persist(category);

        Logger.info("DBpedia data loaded.");

    }

    public static void loadCategory(Category category) {

        category.setNameEN("Movies");
        category.setNameDE("Filmen");

        List<Question> questions = new ArrayList<Question>();

        String[] actors = new String[] {"Johnny_Depp", "Al_Pacino", "Jared_Leto", "Quentin_Tarantino", "Brad_Pitt"} ;
        String[] directors = new String[] {"Tim_Burton", "Francis_Ford_Coppola", "Darren_Aronofsky", "Quentin_Tarantino","David_Fincher"};

        for(int i = 0; i < Math.min(actors.length, directors.length); i++) {
            Question question = new Question();
            loadQuestion(question, actors[i],directors[i]);
            question.setCategory(category);
            questions.add(question);
        }
        category.setQuestions(questions);
    }

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

        question.setTextDE("In welchen Filmen hat " + germanActorName + " gespielt und " + germanDirectorName + " Regie geführt?");
        question.setTextEN("Which movies directed by " + englishDirectorName + " starred " + englishActorName + "?");
        question.setMaxTime(new BigDecimal(40));

        // build SPARQL-query
        SelectQueryBuilder movieQuery = DBPediaService.createQueryBuilder()
                .setLimit(5) // at most five statements
                .addWhereClause(RDF.type, DBPediaOWL.Film)
                .addPredicateExistsClause(FOAF.name)
                .addWhereClause(DBPediaOWL.director, director)
                .addFilterClause(RDFS.label, Locale.GERMAN)
                .addFilterClause(RDFS.label, Locale.ENGLISH);

        // retrieve data from dbpedia
        Model movies = DBPediaService.loadStatements(movieQuery.toQueryString());

        // get english and german movie names, e.g., for right choices
        List<String> englishMovieNames = DBPediaService.getResourceNames(movies, Locale.ENGLISH);
        List<String> germanMovieNames = DBPediaService.getResourceNames(movies, Locale.GERMAN);


        for(int i = 0; i < englishMovieNames.size(); i++) {
            Choice correctChoice = new Choice();
            correctChoice.setTextEN(englishMovieNames.get(i).toString());
            correctChoice.setTextDE(germanMovieNames.get(i).toString());
            question.addRightChoice(correctChoice);
        }
    }
}
