//
// Sloth: An Energy-Efficient Activity Recognition System
// Copyright 2016 Pejman Ghorbanzade <mail@ghorbanzade.com>
// Released under the terms of MIT License
// https://github.com/ghorbanzade/sloth/blob/master/LICENSE
//

package com.ghorbanzade.sloth;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;

/**
 * This class defines an activity as a body posture constructed from activity
 * codes of different nodes, that is assigned a name. An activity is abstract
 * and must be declared whether it is a learned or classified instance.
 *
 * @author Pejman Ghorbanzade
 * @see Classifier
 * @see Learner
 */
public abstract class Activity {

  private final Date date = new Date();
  private final String name;
  private final double accuracy;
  private final Map<Node, ActivityCode> posture;
  private static final Logger log = Logger.getLogger(Activity.class);
  private static final Config cfg =
      ConfigManager.get("config/main.properties");

  /**
   * Creates a learned activity instance based on the body posture during
   * observation of the activity.
   *
   * @param name name of this activity
   * @param posture body posture during performance of observed activity
   */
  public Activity(String name, Map<Node, ActivityCode> posture) {
    this.name = name;
    this.accuracy = 100;
    this.posture = posture;
  }

  /**
   * Creates a classified activity instance based on its prediction accuracy.
   *
   * @param name name of this activity
   * @param accuracy accuracy with which the activity is classified
   */
  public Activity(String name, double accuracy) {
    this.name = name;
    this.posture = null;
    this.accuracy = accuracy;
  }

  /**
   * Writes data regarding this activity into a json file.
   */
  public abstract void log();

  /**
   * Returns name of this activity.
   *
   * @return name of this activity
   */
  public String getName() {
    return this.name;
  }

  /**
   * Returns the accuracy in percent with which this activity has been
   * classified.
   *
   * @return recognition accuracy of this activity
   */
  public double getAccuracy() {
    return this.accuracy;
  }

  /**
   * Returns the date this activity was constructed.
   *
   * @return the date this activity was constructed
   */
  public Date getDate() {
    return (Date) this.date.clone();
  }

  /**
   * Returns the activity code the describes body posture during this
   * activity.
   *
   * @return a map of the codes received from sensor nodes
   */
  public Map<Node, ActivityCode> getPosture() {
    return this.posture;
  }

  /**
   * Returns a string representation of this activity, suitable for printing
   * to standard output.
   *
   * @return a string representation of this activity
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(this.name);
    return sb.toString();
  }

  /**
   * Defines an activity instance that is classified based on the activities
   * previously learned by the system.
   *
   * @author Pejman Ghorbanzade
   * @see Learned
   */
  public static class Classified extends Activity {

    /**
     * Creates an activity object based on observed data via comparing it
     * with the activities previously learned by the system.
     *
     * @param name name of activity that is learned
     * @param accuracy the accuracy with which the activity is predicted
     */
    public Classified(String name, double accuracy) {
      super(name, accuracy);
    }

    /**
     * Records a classified activity in a directory specified in
     * configuration file using the JSON format.
     */
    @Override
    public void log() {
      try {
        File dir = new File(cfg.getAsString("dir.classified.activities"));
        FileUtils.forceMkdir(dir);
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmmss'.json'");
        File file = FileUtils.getFile(dir, sdf.format(new Date()));
        Gson gson = new GsonBuilder().registerTypeAdapter(
            Activity.Learned.class, new Activity.Serializer()
        ).create();
        String content = gson.toJson(this);
        FileUtils.writeStringToFile(file, content, "UTF-8");
      } catch (IOException ex) {
        log.error("unable to store the classifed activity instance");
      }
    }

  }

  /**
   * Defines an activity instance that is learned trough real-time data
   * collection in a laboratory setting.
   *
   * @author Pejman Ghorbanzade
   * @see Classified
   */
  public static class Learned extends Activity {

    /**
     * Loads all learned models as a list of learned activities to be used
     * later for classification.
     *
     * @return a list of activities previously learned
     * @throws IOException if unable to load and parse activity models
     */
    public static ArrayList<Activity> loadAll() throws IOException {
      File dir = new File(cfg.getAsString("dir.learned.activities"));
      FileUtils.forceMkdir(dir);
      String[] exts = {"json"};
      Iterator<File> it = FileUtils.iterateFiles(dir, exts, true);
      ArrayList<Activity> acts = new ArrayList<Activity>();
      Gson gson = new GsonBuilder().registerTypeAdapter(
          Activity.Learned.class, new Activity.Deserializer()
      ).create();
      while (it.hasNext()) {
        File file = it.next();
        String content = FileUtils.readFileToString(file, "UTF-8");
        Activity act = gson.fromJson(content, Activity.Learned.class);
        acts.add(act);
      }
      return acts;
    }

    /**
     * Creates an activity object based on data obtained via real-time model
     * training.
     *
     * @param name name of activity that is learned
     * @param posture the object describing body posture during this activity
     */
    public Learned(String name, Map<Node, ActivityCode> posture) {
      super(name, posture);
    }

    /**
     * Records the activity in a directory specified in configuration file
     * using the JSON format.
     */
    @Override
    public void log() {
      try {
        File dir = new File(cfg.getAsString("dir.learned.activities"));
        FileUtils.forceMkdir(dir);
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmmss'.json'");
        File file = FileUtils.getFile(dir, sdf.format(new Date()));
        Gson gson = new GsonBuilder().registerTypeAdapter(
            Activity.Learned.class, new Activity.Serializer()
        ).create();
        String content = gson.toJson(this);
        FileUtils.writeStringToFile(file, content, "UTF-8");
      } catch (IOException ex) {
        log.error("unable to store the activity instance");
      }
    }

  }

  /**
   * This class defines how an activity instance, learned or classified,
   * should be expressed in json format.
   *
   * @author Pejman Ghorbanzade
   * @see Deserializer
   */
  public static class Serializer implements JsonSerializer<Activity> {

    /**
     * Returns a json element suitable to be used to construct the file that
     * logs an activity.
     *
     * @param act the activity object which should be serialized
     * @param type type of the class being serialized
     * @param context context for serialization
     */
    @Override
    public JsonElement serialize(Activity act, Type type,
        JsonSerializationContext context
    ) {
      if (act instanceof Activity.Learned) {
        return serializeLearned(act);
      } else if (act instanceof Activity.Classified) {
        return serializeClassified(act);
      } else {
        throw new UnsupportedOperationException();
      }
    }

    /**
     * Returns a json element for the given activity which will determine the
     * content of the file to be created for that learned activity instance.
     *
     * @param act the activity object which should be serialized
     * @return a json element with structure suitable to be written to file
     */
    private JsonElement serializeLearned(Activity act) {
      JsonObject obj = new JsonObject();
      obj.addProperty("name", act.getName());
      JsonArray posture = new JsonArray();
      for (Node node: act.getPosture().keySet()) {
        JsonObject part = new JsonObject();
        part.addProperty("id", node.getId());
        JsonArray code = new JsonArray();
        for (double num: act.getPosture().get(node).getCode()) {
          code.add(Math.round(num * 1000) / 1000.0);
        }
        part.add("code", code);
        posture.add(part);
      }
      obj.add("posture", posture);
      return obj;
    }

    /**
     * Returns a json element for the given activity which will determine the
     * content of the file to be created for that classified activity instance.
     *
     * @param act the activity object which should be serialized
     * @return a json element with structure suitable to be written to file
     */
    private JsonElement serializeClassified(Activity act) {
      SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
      df.setTimeZone(TimeZone.getTimeZone("UTC"));
      JsonObject obj = new JsonObject();
      obj.addProperty("name", act.getName());
      obj.addProperty("accuracy", act.getAccuracy());
      obj.addProperty("date", df.format(new Date()));
      return obj;
    }

  }

  /**
   * This class defines how a given string in json format can be converted
   * to an activity object.
   *
   * @author Pejman Ghorbanzade
   * @see Serializer
   */
  public static class Deserializer implements JsonDeserializer<Activity> {

    /**
     * Returns an activity object whose posture can be used by classifier
     * for similarity measurement purposes.
     *
     * @param json json element that should be deserialized
     * @param type type of the json string
     * @param context context for deserialization
     * @return a new activity object that matches given json string
     * @throws JsonParseException if it fails to parse string to activity object
     */
    @Override
    public Activity deserialize(JsonElement json, Type type,
        JsonDeserializationContext context) throws JsonParseException {
      Wsn wsn = WsnManager.getWsn(cfg.getAsString("config.file.wsn"));
      JsonObject obj = json.getAsJsonObject();
      String name = obj.get("name").getAsString();
      Iterator<JsonElement> it = obj.get("posture").getAsJsonArray().iterator();
      HashMap<Node, ActivityCode> hm = new HashMap<Node, ActivityCode>();
      while (it.hasNext()) {
        JsonElement element = it.next();
        int id = element.getAsJsonObject().get("id").getAsInt();
        Node node = wsn.getNode(id);
        JsonArray arr = element.getAsJsonObject().get("code").getAsJsonArray();
        double[] code = new double[arr.size()];
        Iterator<JsonElement> codeIterator = arr.iterator();
        int index = 0;
        while (codeIterator.hasNext()) {
          code[index++] = codeIterator.next().getAsDouble();
        }
        hm.put(node, new ActivityCode(node, code));
      }
      return new Activity.Learned(name, hm);
    }
  }

}
