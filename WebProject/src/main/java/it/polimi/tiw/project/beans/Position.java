package it.polimi.tiw.project.beans;

public enum Position {
    ADMIN,
    /**the user is both manager and collaborator*/
    TECHNICIAN,
    MANAGER,
    COLLABORATOR;
    
	/** 
	 * this method check whether the position is an existent one
	 * 
	 * */
    public static Position parsePosition(String position) {
        for (Position r : Position.values()) {
            if (r.name().equalsIgnoreCase(position)) {
                return r;
            }
        }
        return null;
    }
}
