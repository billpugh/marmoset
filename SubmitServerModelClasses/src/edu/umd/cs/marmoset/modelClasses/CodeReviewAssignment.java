/**
 * Marmoset: a student project snapshot, submission, testing and code review
 * system developed by the Univ. of Maryland, College Park
 * 
 * Developed as part of Jaime Spacco's Ph.D. thesis work, continuing effort led
 * by William Pugh. See http://marmoset.cs.umd.edu/
 * 
 * Copyright 2005 - 2011, Univ. of Maryland
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 */

package edu.umd.cs.marmoset.modelClasses;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.annotation.meta.TypeQualifier;

import edu.umd.cs.marmoset.utilities.SqlUtilities;

public class CodeReviewAssignment {
    
    public enum Kind {
        INSTRUCTIONAL("Instructional code review"), 
        INSTRUCTIONAL_BY_SECTION("Instructional code review by section"),  
        PEER("Peer code review"),
        PEER_BY_SECTION("Peer code review by section"),
        EXEMPLAR("Exemplar/example review"),
        INSTRUCTIONAL_PROTOTYPE("Instructional prototype"), 
        PEER_PROTOTYPE("Peer review prototype");
        					
        private Kind(String description) {
			this.description = description;
		}
        
        final String description;
        
        
		public String getDescription() {
        	return description;
        }
        public boolean isPrototype() {
            return this == INSTRUCTIONAL_PROTOTYPE || this == PEER_PROTOTYPE;
        }
        
        public boolean isByStudents() {
            return this == PEER || this == PEER_BY_SECTION || this == EXEMPLAR || this == PEER_PROTOTYPE;
        }
        
        static Map<String, Kind> normalize = new HashMap<String,Kind>();
        static {
            for(Kind k : values()) {
                String name = k.name();
                normalize.put(name, k);
                if (name.contains("_"))
                    normalize.put(name.replaceAll("_", ""), k);
            }
        }
        
        public static Kind getByParamValue(String value) {
            value = value.toUpperCase();
            Kind k = normalize.get(value);
            if (k == null)
                throw new IllegalArgumentException("No kind type " + value);
            return k;
           
        };
    }
        
	@Documented
	@TypeQualifier(applicableTo = Integer.class)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface PK {}

	public static @PK int asPK(int pk) {
		return pk;
	}
	public static @PK Integer asPK(Integer pk) {
		return pk;
	}
	public static final String TABLE_NAME = "code_review_assignment";

	/**
	 * List of all attributes for courses table.
	 */
     static final String[] ATTRIBUTE_NAME_LIST = {
            "code_review_assignment_pk",
            "project_pk",
            "description",
            "deadline",
            "other_reviews_visible",
            "anonymous", "kind", "visible_to_students"
	};

	/**
	 * Fully-qualified attributes for courses table.
	 */
	 public static final String ATTRIBUTES = Queries.getAttributeList(TABLE_NAME,
			ATTRIBUTE_NAME_LIST);

	private final @CodeReviewAssignment.PK int codeReviewAssignmentPK; // non-NULL, autoincrement
	private final @Project.PK int projectPK;
	private String description;
	private Timestamp deadline;
	private  boolean otherReviewsVisible;
	private  boolean anonymous;
	private  boolean visibleToStudents;
	private  Kind kind;

	public Kind getKind() {
	    return kind;
	}
	public void setKind(Kind kind) {
        this.kind = kind;
    }
	
	public boolean isPrototype() {
	    return kind.isPrototype();
	}
	public boolean isByStudents() {
        return kind.isByStudents();
    }
	public boolean isVisibleToStudents() {
	    boolean result = visibleToStudents && !kind.isPrototype();
	    return result;
	}
	public void setVisibleToStudents(boolean visibleToStudents) {
		this.visibleToStudents = visibleToStudents;
	}
	public Timestamp getDeadline() {
		return deadline;
	}

	public void setDeadline(Timestamp deadline) {
		this.deadline = deadline;
	}

	public boolean isOtherReviewsVisible() {
		return otherReviewsVisible;
	}

	public void setOtherReviewsVisible(boolean otherReviewsVisible) {
		this.otherReviewsVisible = otherReviewsVisible;
	}

	public boolean isAnonymous() {
		return anonymous;
	}

	public void setAnonymous(boolean anonymous) {
		this.anonymous = anonymous;
	}

	public @CodeReviewAssignment.PK int getCodeReviewAssignmentPK() {
		return codeReviewAssignmentPK;
	}

	public @Project.PK int getProjectPK() {
		return projectPK;
	}

	public String getDescription() {
		return description;
	}

    public void setDescription(String description) {
        this.description = description;
    }

	public  CodeReviewAssignment(Connection conn, @Project.PK int projectPK, String description,
			Timestamp deadline, boolean areOtherReviewsVisible, boolean anonymous, Kind kind)
	throws SQLException
	{
	    String insert = Queries.makeInsertStatementUsingSetSyntax(ATTRIBUTE_NAME_LIST, TABLE_NAME, true);

	    this.projectPK = projectPK;
	    this.description = description;
	    this.otherReviewsVisible = areOtherReviewsVisible;
	    this.deadline = deadline;
	    this.anonymous = anonymous;
	    this.kind = kind;
	    this.visibleToStudents = false;
	    PreparedStatement stmt = null;
	    try {
	        stmt = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS);
	        int col = 1;
	        putValues(stmt, col);
	        stmt.executeUpdate();

	        this.codeReviewAssignmentPK = asPK(Queries.getGeneratedPrimaryKey(stmt));
	    } finally {
	        Queries.closeStatement(stmt);
	    }
	}
    /**
     * @param stmt
     * @param col
     * @throws SQLException
     */
    private int putValues(PreparedStatement stmt, int col) throws SQLException {
        stmt.setInt(col++, getProjectPK());
        stmt.setString(col++, getDescription());
        stmt.setTimestamp(col++, getDeadline());
        stmt.setBoolean(col++, isOtherReviewsVisible());
        stmt.setBoolean(col++, isAnonymous());
        stmt.setString(col++, kind.name());
        stmt.setBoolean(col++, visibleToStudents);
        return col;
    }

	public  CodeReviewAssignment(ResultSet resultSet, int startingFrom)
	throws SQLException
	{
		this.codeReviewAssignmentPK = CodeReviewAssignment.asPK(
				resultSet.getInt(startingFrom++));
		this.projectPK = Project.asPK(resultSet.getInt(startingFrom++));
		this.description = resultSet.getString(startingFrom++);
		this.deadline = resultSet.getTimestamp(startingFrom++);
		this.otherReviewsVisible = resultSet.getBoolean(startingFrom++);
		this.anonymous = resultSet.getBoolean(startingFrom++);
		this.kind = Kind.valueOf(resultSet.getString(startingFrom++));
		this.visibleToStudents = resultSet.getBoolean(startingFrom++);
	}
	
	
	public void delete(Connection conn) throws SQLException {
	String rubricTables = Rubric.TABLE_NAME + "," + RubricEvaluation.TABLE_NAME;

		executeDeleteCodeReviewAssignment(conn, "DELETE " + rubricTables + " FROM " + rubricTables  
				+ " WHERE code_review_assignment_pk = ? "
				+ " AND " + Rubric.TABLE_NAME+".rubic_pk = "
				+ RubricEvaluation.TABLE_NAME+".rubic_pk");	
		
		String commentTables = CodeReviewComment.TABLE_NAME + "," + CodeReviewThread.TABLE_NAME
		    +"," + CodeReviewer.TABLE_NAME;

		executeDeleteCodeReviewAssignment(conn, "DELETE " + commentTables + " FROM " + commentTables  
        + " WHERE code_review_assignment_pk = ? "
        + " AND " + CodeReviewComment.TABLE_NAME+".code_review_thread_pk = "
        + CodeReviewThread.TABLE_NAME+".code_review_thread_pk"
            + "AND " + CodeReviewComment.TABLE_NAME+".code_reviewer_pk = " +
             CodeReviewer.TABLE_NAME+".code_reviewer_pk"); 
  
		executeDeleteCodeReviewAssignment(conn, "DELETE FROM " + Rubric.TABLE_NAME
        + " WHERE code_review_assignment_pk = ?");

		executeDeleteCodeReviewAssignment(conn, "DELETE FROM " + CodeReviewer.TABLE_NAME
        + " WHERE code_review_assignment_pk = ?");
 
		executeDeleteCodeReviewAssignment(conn, "DELETE FROM " + CodeReviewAssignment.TABLE_NAME
				+ " WHERE code_review_assignment_pk = ?");
		
	}
	private void executeDeleteCodeReviewAssignment(Connection conn, String cmd)
			throws SQLException {
		PreparedStatement stmt = conn.prepareStatement(cmd);
		try {
			Queries.setStatement(stmt, codeReviewAssignmentPK);
			stmt.execute();
		} finally {
			stmt.close();
		}
	}
    public void update(Connection conn) throws SQLException {
        String whereClause = " WHERE code_review_assignment_pk = ? ";

        String update = Queries.makeUpdateStatementWithWhereClause(ATTRIBUTE_NAME_LIST, TABLE_NAME, whereClause);

        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(update);
            int index = putValues(stmt, 1);
            SqlUtilities.setInteger(stmt, index, getCodeReviewAssignmentPK());
            stmt.executeUpdate();
        } finally {
            Queries.closeStatement(stmt);
        }
    }
    
    public static List<CodeReviewAssignment> lookupAllUpcoming(Timestamp time,
            Connection conn) throws SQLException
    {
        String query = "SELECT " + ATTRIBUTES + " FROM " + TABLE_NAME +
        " WHERE deadline > ? "+
        " ORDER BY deadline ASC ";


        return getFromPreparedStatement( Queries.setStatement(conn, query, time));
       
    }
    

    public static List<CodeReviewAssignment> lookupAll(Connection conn)
            throws SQLException {
        String query = "SELECT " + ATTRIBUTES + " FROM " + TABLE_NAME;

        PreparedStatement stmt = conn.prepareStatement(query);
        return getFromPreparedStatement(stmt);

    }

    private static List<CodeReviewAssignment> getFromPreparedStatement(
            PreparedStatement stmt) throws SQLException {
        try {
            List<CodeReviewAssignment> assignments = new LinkedList<CodeReviewAssignment>();
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                assignments.add(new CodeReviewAssignment(rs, 1));
            }
            return assignments;
        } finally {
            stmt.close();
        }
    }
    
	public static @CheckForNull CodeReviewAssignment lookupByPK(
			@CodeReviewAssignment.PK int codeReviewAssignmentPK,
			Connection conn) throws SQLException {
		if (codeReviewAssignmentPK == 0)
			return null;
		String query = "SELECT " + ATTRIBUTES + " FROM " + TABLE_NAME
				+ " WHERE code_review_assignment_pk = ? ";

		PreparedStatement stmt = conn.prepareStatement(query);
		try {
			stmt.setInt(1, codeReviewAssignmentPK);
			ResultSet rs = stmt.executeQuery();
			if (rs.next())
				return new CodeReviewAssignment(rs, 1);
			return null;
		} finally {
			stmt.close();
		}
	}

	public static Collection<CodeReviewAssignment> lookupByProjectPK(
			@Project.PK int projectPK,
			Connection conn) throws SQLException {
		String query = "SELECT " + ATTRIBUTES + " FROM " + TABLE_NAME
				+ " WHERE project_pk = ? ";

		PreparedStatement stmt = conn.prepareStatement(query);
		try {
			stmt.setInt(1, projectPK);
			ResultSet rs = stmt.executeQuery();
			LinkedList<CodeReviewAssignment> result = new LinkedList<CodeReviewAssignment>();

			while (rs.next())
				result.add( new CodeReviewAssignment(rs, 1));
			return result;
		} finally {
			stmt.close();
		}
	}
}
