package com.makmur.controller;

import com.makmur.entity.Product;
import com.makmur.repository.ProductRepository;
import com.makmur.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository repo;
    private final AuthService authService;

    public ProductController(ProductRepository repo, AuthService authService) {
        this.repo = repo;
        this.authService = authService;
    }

    @GetMapping
    public List<Product> listAll() {
        authService.requireRole("admin", "staff");
        return repo.findAllByOrderByNameAsc();
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam("q") String query) {
        authService.requireRole("admin", "staff");
        if (query == null || query.length() < 2) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("error", "validation_error");
            Map<String, String> fields = new LinkedHashMap<>();
            fields.put("q", "Search query must be at least 2 characters");
            body.put("fields", fields);
            return ResponseEntity.status(422).body(body);
        }
        List<Product> results = repo.searchByName(query);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{barcode}")
    public ResponseEntity<?> getByBarcode(@PathVariable String barcode) {
        authService.requireRole("admin", "staff");
        Optional<Product> product = repo.findByBarcode(barcode);
        if (product.isEmpty()) {
            Map<String, String> body = new LinkedHashMap<>();
            body.put("error", "not_found");
            body.put("message", "Product with barcode " + barcode + " not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }
        return ResponseEntity.ok(product.get());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        authService.requireRole("admin", "staff");
        // Validate fields
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        String barcode = body.get("barcode") != null ? body.get("barcode").toString() : null;
        String name = body.get("name") != null ? body.get("name").toString() : null;
        Object priceObj = body.get("price");
        Object stockObj = body.get("stock");

        if (barcode == null || !barcode.matches("\\d{13}")) {
            fieldErrors.put("barcode", "Barcode must be exactly 13 digits");
        }
        if (name == null || name.trim().isEmpty()) {
            fieldErrors.put("name", "Product name is required");
        }
        if (priceObj == null) {
            fieldErrors.put("price", "Price must be a positive number");
        } else {
            try {
                double price = Double.parseDouble(priceObj.toString());
                if (price <= 0) {
                    fieldErrors.put("price", "Price must be a positive number");
                }
            } catch (NumberFormatException e) {
                fieldErrors.put("price", "Price must be a positive number");
            }
        }
        if (stockObj != null) {
            try {
                int stock = Integer.parseInt(stockObj.toString());
                if (stock < 0) {
                    fieldErrors.put("stock", "Stock must be a non-negative integer");
                }
            } catch (NumberFormatException e) {
                fieldErrors.put("stock", "Stock must be a non-negative integer");
            }
        }

        if (!fieldErrors.isEmpty()) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "validation_error");
            err.put("fields", fieldErrors);
            return ResponseEntity.status(422).body(err);
        }

        // Check for duplicate barcode
        Optional<Product> existing = repo.findByBarcode(barcode);
        if (existing.isPresent()) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "duplicate_barcode");
            err.put("message", "Product with barcode " + barcode + " already exists");
            err.put("existing_product", existing.get());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(err);
        }

        // Create
        double price = Double.parseDouble(priceObj.toString());
        int stock = stockObj != null ? Integer.parseInt(stockObj.toString()) : 0;

        Product product = new Product(barcode, name.trim(), price, stock);
        Product saved = repo.save(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PatchMapping("/{barcode}/stock")
    public ResponseEntity<?> updateStock(@PathVariable String barcode, @RequestBody Map<String, Object> body) {
        authService.requireRole("admin", "staff");
        Optional<Product> existing = repo.findByBarcode(barcode);
        if (existing.isEmpty()) {
            Map<String, String> err = new LinkedHashMap<>();
            err.put("error", "not_found");
            err.put("message", "Product " + barcode + " not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err);
        }

        Product product = existing.get();
        Object valueObj = body.get("value");
        Object deltaObj = body.get("delta");

        int newStock;
        if (valueObj != null) {
            try {
                newStock = Integer.parseInt(valueObj.toString());
            } catch (NumberFormatException e) {
                Map<String, Object> err = new LinkedHashMap<>();
                err.put("error", "validation_error");
                Map<String, String> fields = new LinkedHashMap<>();
                fields.put("stock", "Stock value must be an integer");
                err.put("fields", fields);
                return ResponseEntity.status(422).body(err);
            }
        } else if (deltaObj != null) {
            try {
                newStock = product.getStock() + Integer.parseInt(deltaObj.toString());
            } catch (NumberFormatException e) {
                Map<String, Object> err = new LinkedHashMap<>();
                err.put("error", "validation_error");
                Map<String, String> fields = new LinkedHashMap<>();
                fields.put("stock", "Delta must be an integer");
                err.put("fields", fields);
                return ResponseEntity.status(422).body(err);
            }
        } else {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "validation_error");
            Map<String, String> fields = new LinkedHashMap<>();
            fields.put("stock", "Provide \"value\" (absolute) or \"delta\" (change)");
            err.put("fields", fields);
            return ResponseEntity.status(422).body(err);
        }

        if (newStock < 0) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "validation_error");
            Map<String, String> fields = new LinkedHashMap<>();
            fields.put("stock", "Stock cannot be negative");
            err.put("fields", fields);
            return ResponseEntity.status(422).body(err);
        }

        int previousStock = product.getStock();
        product.setStock(newStock);
        repo.save(product);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("barcode", barcode);
        response.put("stock", newStock);
        response.put("previous_stock", previousStock);
        return ResponseEntity.ok(response);
    }
}