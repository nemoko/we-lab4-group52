package controllers;

import highscore.PublishHighScoreServiceClient;
import models.*;
import play.Logger;
import play.Play;
import play.api.Application;
import play.api.cache.Cache;
import play.data.DynamicForm;
import play.data.Form;
import play.db.jpa.Transactional;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import scala.Option;
import twitter.PublishHighScoreTwitterClient;
import twitter.TwitterStatusMessage;
import views.html.quiz.index;
import views.html.quiz.quiz;
import views.html.quiz.quizover;
import views.html.quiz.roundover;

import java.util.*;

@Security.Authenticated(Secured.class)
public class Quiz extends Controller {

	public static Result index() {
		return ok(index.render());
	}

	@play.db.jpa.Transactional(readOnly = true)
	public static Result newGame() {
		createNewGame();
		return redirect(routes.Quiz.question());
	}

	@play.db.jpa.Transactional(readOnly = true)
	private static QuizGame createNewGame() {
		List<Category> allCategories = QuizDAO.INSTANCE.findEntities(Category.class);
		Logger.info("Start game with " + allCategories.size() + " categories.");
		QuizGame game = new QuizGame(allCategories);
		game.startNewRound();
		cacheGame(game);
		return game;
	}

	private static String dataFilePath() {
		return Play.application().configuration().getString("questions.filePath");
	}

	private static QuizUser user() {
		String userId = Secured.getAuthentication(session());
		return QuizDAO.INSTANCE.findById(Long.valueOf(userId));
	}

	@play.db.jpa.Transactional(readOnly = true)
	public static Result question() {
		QuizGame game = cachedGame();
		if (currentQuestion(game) != null) {
			return ok(quiz.render(game));
		} else {
			return badRequest(Messages.get("quiz.no-current-question"));
		}
	}

	@Transactional(readOnly = true)
	private static Question currentQuestion(QuizGame game) {
		if (game != null && game.getCurrentRound() != null) {
			QuizUser user = game.getPlayers().get(0);
			return game.getCurrentRound().getCurrentQuestion(user);
		} else {
			return null;
		}
	}

	@play.db.jpa.Transactional(readOnly = true)
	public static Result addAnswer() {
		QuizGame game = cachedGame();
		Question question = currentQuestion(game);
		if (question != null) {
			processAnswerIfSent(game);
			return redirectAccordingToGameState(game);
		} else {
			return badRequest(Messages.get("quiz.no-current-question"));
		}
	}

	@Transactional
	private static void processAnswerIfSent(QuizGame game) {
		DynamicForm form = Form.form().bindFromRequest();
		QuizUser user = game.getPlayers().get(0);
		Question question = game.getCurrentRound().getCurrentQuestion(user);
		int sentQuestionId = Integer.valueOf(form.data().get("questionid"));
		if (question.getId() == sentQuestionId) {
			List<Choice> choices = obtainSelectedChoices(form, question);
			long time = Long.valueOf(form.get("timeleft"));
			game.answerCurrentQuestion(user, choices, time);
		}
	}

	@Transactional
	private static List<Choice> obtainSelectedChoices(DynamicForm form,
			Question question) {
		Map<String, String> formData = form.data();
		List<Choice> choices = new ArrayList<Choice>();
		int i = 0;
		String chosenId = null;
		while ((chosenId = formData.get("choices[" + i + "]")) != null) {
			Choice choice = getChoiceById(Integer.valueOf(chosenId), question);
			if (choice != null) {
				choices.add(choice);
			}
			i++;
		}
		return choices;
	}

	private static Choice getChoiceById(int id, Question question) {
		for (Choice choice : question.getChoices())
			if (id == choice.getId())
				return choice;
		return null;
	}

	private static Result redirectAccordingToGameState(QuizGame game) {
		if (isRoundOver(game)) {
			return redirect(routes.Quiz.roundResult());
		} else if (isGameOver(game)) {
            return redirect(routes.Quiz.endResult());
		} else {
			return redirect(routes.Quiz.question());
		}
	}

    private static String publishHighScore(QuizGame g){
        //--- Highscore service call ---
        QuizUser winner = g.getWinner();
        QuizUser user1 = g.getPlayers().get(0);
        QuizUser user2 = g.getPlayers().get(1);
        String firstName1 = "epicWin3017";
        String firstName2 = "that";
        String lastName1 = "mustNoBeEmpty(doh)";
        String lastName2 = "bot";
        String birthdate1 = "1000-10-10";
        String birthdate2 = "1000-10-10";
        String gender1 = "male";
        String gender2 = "male";
        String status1;
        String status2;

        if(user1.getFirstName()!=null)
            firstName1= user1.getFirstName();
        if(user2.getFirstName()!=null)
            firstName2= user2.getFirstName();
        if(user1.getLastName()!=null)
            lastName1= user1.getLastName();
        if(user1.getBirthDate()!=null)
            birthdate1 = user1.getBirthDate().toString();
        if(user1.getGender()!=null)
            gender1 = user1.getGender().toString();
        if(user2.getLastName()!=null)
            lastName2= user2.getLastName();
        if(user2.getBirthDate()!=null)
            birthdate2 = user2.getBirthDate().toString();
        if(user2.getGender()!=null)
            gender2 = user2.getGender().toString();

        if(!winner.equals(null)){
            if(winner.getName().equals("Spieler 2")){
                status1 = "loser";
                status2 = "winner";
            } else {
                status2 = "loser";
                status1 = "winner";
            }
            PublishHighScoreServiceClient client = new PublishHighScoreServiceClient(firstName1,lastName1,birthdate1,gender1,status1,firstName2,lastName2,birthdate2,gender2,status2);
            String uuid = client.fire();
            PublishHighScoreTwitterClient twitterClient = new PublishHighScoreTwitterClient();
            TwitterStatusMessage message = new TwitterStatusMessage("myPC",uuid,new Date());

            try {
                twitterClient.publishUuid(message);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return uuid;
        }
        return null;
    }

	private static boolean isGameOver(QuizGame game) {
		return game.isRoundOver() && game.isGameOver();
	}

	private static boolean isRoundOver(QuizGame game) {
		return game.isRoundOver() && !game.isGameOver();
	}

	private static void cacheGame(QuizGame game) {
		Cache.set(gameId(), game, 3600, application());
	}

	@play.db.jpa.Transactional(readOnly = true)
	public static Result roundResult() {
		QuizGame game = cachedGame();
		if (game != null && isRoundOver(game)) {
			return ok(roundover.render(game));
		} else {
			return badRequest(Messages.get("quiz.no-round-result"));
		}
	}

	@play.db.jpa.Transactional(readOnly = true)
	public static Result endResult() {
		QuizGame game = cachedGame();
		if (game != null && isGameOver(game)) {
            String uuid = publishHighScore(game);
			return ok(quizover.render(game, uuid));
		} else {
			return badRequest(Messages.get("quiz.no-end-result"));
		}
	}

	@play.db.jpa.Transactional(readOnly = true)
	public static Result newRound() {
		QuizGame game = cachedGame();
		if (game != null && isRoundOver(game)) {
			game.startNewRound();
			return redirect(routes.Quiz.question());
		} else {
			return badRequest(Messages.get("quiz.no-round-ended"));
		}
	}

	private static QuizGame cachedGame() {
		Option<Object> option = Cache.get(gameId(), application());
		if (option.isDefined() && option.get() instanceof QuizGame) {
			return (QuizGame) option.get();
		} else {
			return createNewGame();
		}
	}

	private static String gameId() {
		return "game." + uuid();
	}

	private static String uuid() {
		String uuid = session("uuid");
		if (uuid == null) {
			uuid = UUID.randomUUID().toString();
			session("uuid", uuid);
		}
		return uuid;
	}

	private static Application application() {
		return Play.application().getWrappedApplication();
	}

}
