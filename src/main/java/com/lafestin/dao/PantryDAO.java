package com.lafestin.dao;

import com.lafestin.model.PantryItem;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PantryDAO {
    public List<PantryItem> getPantryByUser(int userId)
        throws SQLException {
        return new ArrayList<>(); // TODO: implement
    }
    public void deletePantryItem(int ingredientId, int userId)
        throws SQLException {
        // TODO: implement
    }
}