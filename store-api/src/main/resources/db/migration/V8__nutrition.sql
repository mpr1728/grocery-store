-- Nutrition per 100 g (or 100 ml) as sold, and the net weight of one pack in grams (millilitres for
-- liquids), so the nutrition in a shopping list can be added up. nutrition_source says where the
-- figures came from: LABEL (the pack, via Open Food Facts) or TYPICAL (standard food tables).
ALTER TABLE products ADD COLUMN pack_grams INT;
ALTER TABLE products ADD COLUMN protein_g DECIMAL(6,2);
ALTER TABLE products ADD COLUMN fat_g DECIMAL(6,2);
ALTER TABLE products ADD COLUMN carbs_g DECIMAL(6,2);
ALTER TABLE products ADD COLUMN fiber_g DECIMAL(6,2);
ALTER TABLE products ADD COLUMN kcal DECIMAL(7,2);
ALTER TABLE products ADD COLUMN nutrition_source VARCHAR(20);
