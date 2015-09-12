package prisms.ui;

public class UIUtil {
	/**
	 * Serializes a set of node actions
	 *
	 * @param actions The actions to serialize
	 * @return The serialized actions
	 */
	public static org.json.simple.JSONArray serialize(prisms.ui.list.NodeAction [] actions) {
		org.json.simple.JSONArray ret = new org.json.simple.JSONArray();
		for(prisms.ui.list.NodeAction a : actions) {
			org.json.simple.JSONObject actionObj = new org.json.simple.JSONObject();
			actionObj.put("text", a.getText());
			actionObj.put("multiple", Boolean.valueOf(a.getMultiple()));
			ret.add(actionObj);
		}
		return ret;
	}
}
