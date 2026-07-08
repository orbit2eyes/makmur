package com.makmur.repository;

import com.makmur.entity.Product;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class ProductRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<Product> MAPPER = (rs, rowNum) -> {
        Product p = new Product();
        p.setId(rs.getLong("id"));
        p.setBarcode(rs.getString("barcode"));
        p.setName(rs.getString("name"));
        p.setPrice(rs.getDouble("price"));
        p.setStock(rs.getInt("stock"));
        p.setCreatedAt(rs.getString("created_at"));
        return p;
    };

    public ProductRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Product> findAllByOrderByNameAsc() {
        return jdbc.query("SELECT * FROM products ORDER BY name ASC", MAPPER);
    }

    public Optional<Product> findByBarcode(String barcode) {
        List<Product> results = jdbc.query(
            "SELECT * FROM products WHERE barcode = ?", MAPPER, barcode);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<Product> searchByName(String query) {
        return jdbc.query(
            "SELECT * FROM products WHERE name LIKE '%' || ? || '%' COLLATE NOCASE ORDER BY name ASC",
            MAPPER, query);
    }

    public Product save(Product product) {
        if (product.getId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbc.update(conn -> {
                PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO products (barcode, name, price, stock) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, product.getBarcode());
                ps.setString(2, product.getName());
                ps.setDouble(3, product.getPrice());
                ps.setInt(4, product.getStock());
                return ps;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key != null) {
                product.setId(key.longValue());
            }
            return findByBarcode(product.getBarcode()).orElse(product);
        } else {
            jdbc.update(
                "UPDATE products SET barcode = ?, name = ?, price = ?, stock = ? WHERE id = ?",
                product.getBarcode(), product.getName(), product.getPrice(), product.getStock(), product.getId());
            return product;
        }
    }
}