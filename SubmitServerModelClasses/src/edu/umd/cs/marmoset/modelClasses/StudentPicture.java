package edu.umd.cs.marmoset.modelClasses;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.annotation.CheckForNull;

public class StudentPicture {

	public @Student.PK int getStudentPK() {
		return studentPK;
	}

	public String getType() {
		return type;
	}

	public byte[] getImage() {
		return image;
	}

	public static final String TABLE_NAME = "student_pictures";

	static final String[] ATTRIBUTE_NAME_LIST = { "student_pk", "type", "image" };

	public static final String ATTRIBUTES = Queries.getAttributeList(
			TABLE_NAME, ATTRIBUTE_NAME_LIST);

	final @Student.PK int studentPK;
	final String type;
	final byte[] image;

	public StudentPicture(ResultSet resultSet, int startingFrom)
			throws SQLException {
		studentPK = Student.asPK(resultSet.getInt(startingFrom++));
		type = resultSet.getString(startingFrom++);
		Blob blob = resultSet.getBlob(startingFrom++);
		long length = blob.length();
		if (length > 50000)
			throw new RuntimeException("Picture too big: " + length + " bytes");
		image = blob.getBytes(1, (int) length);
	}

	public static void insertOrUpdate(Connection conn, Student student,
			String type, Blob blob) throws SQLException {
		if (Student.FAKE_NAMES)
			return;
		String query = Queries.makeInsertOrUpdateStatement(ATTRIBUTE_NAME_LIST,
				TABLE_NAME);
		PreparedStatement stmt = conn.prepareStatement(query);
		try {
			Queries.setInsertOrUpdateStatement(stmt, student.getStudentPK(), type, blob);
			stmt.executeUpdate();
		} finally {
			Queries.closeStatement(stmt);
		}
	}

	public static @CheckForNull
	StudentPicture lookupByStudentPK(@Student.PK int studentPK, Connection conn)
			throws SQLException {
		if (Student.FAKE_NAMES)
			return null;
		String query = " SELECT " + ATTRIBUTES + " FROM " + TABLE_NAME
				+ " WHERE student_pk = ? ";

		PreparedStatement stmt = conn.prepareStatement(query);
		stmt.setInt(1, studentPK);
		ResultSet rs = stmt.executeQuery();
		if (rs.next())
			return new StudentPicture(rs, 1);
		return null;
	}

}
