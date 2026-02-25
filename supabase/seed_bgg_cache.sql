-- Seed top 10 BGG games into cache
INSERT INTO bgg_games_cache (bgg_id, name, year_published, thumbnail_url, image_url, min_players, max_players, min_playtime, max_playtime, playing_time, weight, description, categories, mechanics, bgg_rank, num_ratings, average_rating, cached_at)
VALUES
(224517, 'Brass: Birmingham', 2018, 'https://cf.geekdo-images.com/x3zxjr-Vw5iU4yDPg70Jgw__thumb/img/ujCLbVSPFPxgE4vcXrCXeOlZrmw=/fit-in/200x150/filters:strip_icc()/pic3490053.jpg', 'https://cf.geekdo-images.com/x3zxjr-Vw5iU4yDPg70Jgw__original/img/giNUMut4HAl-zWyQkGG0YchmuLI=/0x0/filters:format(jpeg)/pic3490053.jpg', 2, 4, 60, 120, 120, 3.87, 'Brass: Birmingham is an economic strategy game sequel to Martin Wallace''s 2007 masterpiece, Brass. Set during the industrial revolution in Birmingham, England.', ARRAY['Economic', 'Industry / Manufacturing', 'Post-Napoleonic', 'Transportation'], ARRAY['Hand Management', 'Income', 'Loans', 'Market', 'Network and Route Building'], 1, 58000, 8.61, NOW()),

(174430, 'Gloomhaven', 2017, 'https://cf.geekdo-images.com/sZYp_3BTDGjh2unaZfZmuA__thumb/img/veqFeP4d_3zNzMthSW1Yimgh5mg=/fit-in/200x150/filters:strip_icc()/pic2437871.jpg', 'https://cf.geekdo-images.com/sZYp_3BTDGjh2unaZfZmuA__original/img/l1rdkXq6MuScBqZgT3eY6e-z1f0=/0x0/filters:format(jpeg)/pic2437871.jpg', 1, 4, 60, 120, 120, 3.86, 'Gloomhaven is a game of Euro-inspired tactical combat in a persistent world of shifting motives.', ARRAY['Adventure', 'Exploration', 'Fantasy', 'Fighting', 'Miniatures'], ARRAY['Action Queue', 'Campaign / Battle Card Driven', 'Cooperative Game', 'Grid Movement', 'Hand Management', 'Legacy Game', 'Modular Board', 'Multi-Use Cards', 'Role Playing', 'Scenario / Mission / Campaign Game', 'Simultaneous Action Selection', 'Solo / Solitaire Game', 'Variable Player Powers'], 2, 55000, 8.46, NOW()),

(342942, 'Ark Nova', 2021, 'https://cf.geekdo-images.com/SoU8p28Sk1s8MSvoM4N8pQ__thumb/img/a9vVvIgShiwxNrSE3eLUUqTinbI=/fit-in/200x150/filters:strip_icc()/pic6293412.jpg', 'https://cf.geekdo-images.com/SoU8p28Sk1s8MSvoM4N8pQ__original/img/GYXBEHiUXx3Nxj-Kna7Hgh6U8UE=/0x0/filters:format(jpeg)/pic6293412.jpg', 1, 4, 90, 150, 150, 3.70, 'Plan and build a modern, scientifically managed zoo to support conservation projects.', ARRAY['Animals', 'Environmental'], ARRAY['Enclosure', 'End Game Bonuses', 'Hand Management', 'Hexagon Grid', 'Income', 'Open Drafting', 'Set Collection', 'Solo / Solitaire Game', 'Variable Player Powers', 'Variable Set-up'], 3, 42000, 8.54, NOW()),

(167791, 'Terraforming Mars', 2016, 'https://cf.geekdo-images.com/wg9oOLcsKvDesSUdZQ4rxw__thumb/img/BTxqxgYay5tHJLbugfTv-O0SFqc=/fit-in/200x150/filters:strip_icc()/pic3536616.jpg', 'https://cf.geekdo-images.com/wg9oOLcsKvDesSUdZQ4rxw__original/img/klTnp2OWYG3VJK9JnqYk2GlJnUs=/0x0/filters:format(jpeg)/pic3536616.jpg', 1, 5, 120, 120, 120, 3.24, 'Compete with rival corporations to terraform Mars. Build infrastructure, grow forests, and create oceans.', ARRAY['Economic', 'Environmental', 'Industry / Manufacturing', 'Science Fiction', 'Territory Building'], ARRAY['Card Drafting', 'End Game Bonuses', 'Hand Management', 'Hexagon Grid', 'Income', 'Set Collection', 'Solo / Solitaire Game', 'Take That', 'Tile Placement', 'Turn Order: Progressive', 'Variable Player Powers'], 4, 95000, 8.38, NOW()),

(316554, 'Dune: Imperium', 2020, 'https://cf.geekdo-images.com/PhjygpWSo-0labGrPBMyyg__thumb/img/mU-Fn8i0jYfKxC9D0YiI7J4VOMU=/fit-in/200x150/filters:strip_icc()/pic5666597.jpg', 'https://cf.geekdo-images.com/PhjygpWSo-0labGrPBMyyg__original/img/CaPmKO0uQHq7a81dJvYJW5XpULc=/0x0/filters:format(jpeg)/pic5666597.jpg', 1, 4, 60, 120, 120, 3.01, 'Dune: Imperium blends deck-building and worker placement in a deeply thematic experience within Frank Herbert''s universe.', ARRAY['Novel-based', 'Science Fiction'], ARRAY['Deck Bag and Pool Building', 'Force Commitment', 'Hidden Victory Points', 'Solo / Solitaire Game', 'Take That', 'Turn Order: Claim Action', 'Variable Player Powers', 'Worker Placement'], 5, 48000, 8.36, NOW()),

(266192, 'Wingspan', 2019, 'https://cf.geekdo-images.com/yLZJCVLlIx4c7eJEWUNJ7w__thumb/img/VNToqgS2-pOGU6MuvIkMPKn_y-s=/fit-in/200x150/filters:strip_icc()/pic4458123.jpg', 'https://cf.geekdo-images.com/yLZJCVLlIx4c7eJEWUNJ7w__original/img/t-WF1HP0Nzmu_0Y9L_iH0tdRRBw=/0x0/filters:format(jpeg)/pic4458123.jpg', 1, 5, 40, 70, 70, 2.44, 'Attract birds to your wildlife reserves in this engine-building game about bird collection.', ARRAY['Animals', 'Card Game', 'Educational'], ARRAY['Dice Rolling', 'End Game Bonuses', 'Hand Management', 'Set Collection', 'Solo / Solitaire Game'], 6, 88000, 8.06, NOW()),

(169786, 'Scythe', 2016, 'https://cf.geekdo-images.com/7k_nOxpO9OGIjhLq2BUZdA__thumb/img/eQ821VqdXp3C4-Bshl_yNA2YLDY=/fit-in/200x150/filters:strip_icc()/pic3163924.jpg', 'https://cf.geekdo-images.com/7k_nOxpO9OGIjhLq2BUZdA__original/img/aKhEDLCAk7z6ZwOvOMwU4OssuTU=/0x0/filters:format(jpeg)/pic3163924.jpg', 1, 5, 90, 115, 115, 3.42, 'Alternate-history 1920s Europe: mech-powered farming and war in a beautifully illustrated world.', ARRAY['Economic', 'Fighting', 'Science Fiction', 'Territory Building'], ARRAY['Area Majority / Influence', 'Contracts', 'End Game Bonuses', 'Force Commitment', 'Grid Movement', 'Hexagon Grid', 'Solo / Solitaire Game', 'Variable Player Powers'], 7, 75000, 8.18, NOW()),

(162886, 'Spirit Island', 2017, 'https://cf.geekdo-images.com/kjCm4ZvPjIZxS-mYgSPy1g__thumb/img/YABXmqKz_sw8H1P2c-8Rk6Jt2fo=/fit-in/200x150/filters:strip_icc()/pic3615739.jpg', 'https://cf.geekdo-images.com/kjCm4ZvPjIZxS-mYgSPy1g__original/img/flphCMCK7EQ47Gxf0o12d1r3MmI=/0x0/filters:format(jpeg)/pic3615739.jpg', 1, 4, 90, 120, 120, 4.05, 'Spirits of the island unite to defend their home from invading colonizers.', ARRAY['Age of Reason', 'Environmental', 'Fantasy', 'Fighting', 'Mythology', 'Territory Building'], ARRAY['Area Majority / Influence', 'Cooperative Game', 'Hand Management', 'Modular Board', 'Simultaneous Action Selection', 'Solo / Solitaire Game', 'Variable Player Powers'], 8, 52000, 8.27, NOW()),

(233078, 'Twilight Imperium: Fourth Edition', 2017, 'https://cf.geekdo-images.com/op8ixq6lnJyMUlxqgDnZSQ__thumb/img/scIjLBSTYKRGS8REccOMOqeDvo8=/fit-in/200x150/filters:strip_icc()/pic3727516.jpg', 'https://cf.geekdo-images.com/op8ixq6lnJyMUlxqgDnZSQ__original/img/jPTtJHWHLLTsniL5b6H0t3e1sgs=/0x0/filters:format(jpeg)/pic3727516.jpg', 3, 6, 240, 480, 480, 4.22, 'Build an intergalactic empire through trade, warfare, and political maneuvering.', ARRAY['Civilization', 'Negotiation', 'Political', 'Science Fiction', 'Space Exploration', 'Wargame'], ARRAY['Action Drafting', 'Area Majority / Influence', 'Dice Rolling', 'Hexagon Grid', 'Modular Board', 'Tech Trees / Tech Tracks', 'Trading', 'Variable Phase Order', 'Variable Player Powers', 'Voting'], 9, 28000, 8.61, NOW()),

(161936, 'Pandemic Legacy: Season 1', 2015, 'https://cf.geekdo-images.com/QHmcoqXCW7vuuSIbJaoCmQ__thumb/img/6qvANLsfVKoWVNJTaOzQLh3djsM=/fit-in/200x150/filters:strip_icc()/pic2452831.png', 'https://cf.geekdo-images.com/QHmcoqXCW7vuuSIbJaoCmQ__original/img/CJhX45WNwkKqLXdIV1dEvjkVr8A=/0x0/filters:format(png)/pic2452831.png', 2, 4, 60, 60, 60, 2.84, 'Mutating diseases threaten the world in this 12-month cooperative legacy game.', ARRAY['Medical'], ARRAY['Action Points', 'Cooperative Game', 'Hand Management', 'Legacy Game', 'Point to Point Movement', 'Set Collection', 'Trading', 'Variable Player Powers'], 10, 48000, 8.55, NOW())

ON CONFLICT (bgg_id) DO UPDATE SET
  name = EXCLUDED.name,
  year_published = EXCLUDED.year_published,
  thumbnail_url = EXCLUDED.thumbnail_url,
  image_url = EXCLUDED.image_url,
  min_players = EXCLUDED.min_players,
  max_players = EXCLUDED.max_players,
  min_playtime = EXCLUDED.min_playtime,
  max_playtime = EXCLUDED.max_playtime,
  playing_time = EXCLUDED.playing_time,
  weight = EXCLUDED.weight,
  description = EXCLUDED.description,
  categories = EXCLUDED.categories,
  mechanics = EXCLUDED.mechanics,
  bgg_rank = EXCLUDED.bgg_rank,
  num_ratings = EXCLUDED.num_ratings,
  average_rating = EXCLUDED.average_rating,
  cached_at = NOW();
