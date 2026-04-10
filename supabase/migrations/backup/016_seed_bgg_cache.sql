-- Seed top 10 BGG games into cache
INSERT INTO bgg_games_cache (bgg_id, name, year_published, thumbnail_url, min_players, max_players, playing_time, weight, description, categories, mechanics, bgg_rank, num_ratings, average_rating, cached_at)
VALUES
(224517, 'Brass: Birmingham', 2018, 'https://cf.geekdo-images.com/x3zxjr-Vw5iU4yDPg70Jgw__thumb/img/ujCLbVSPFPxgE4vcXrCXeOlZrmw=/fit-in/200x150/filters:strip_icc()/pic3490053.jpg', 2, 4, 120, 3.87, 'Economic strategy game set during the industrial revolution in Birmingham.', ARRAY['Economic'], ARRAY['Hand Management', 'Network Building'], 1, 58000, 8.61, NOW()),
(174430, 'Gloomhaven', 2017, 'https://cf.geekdo-images.com/sZYp_3BTDGjh2unaZfZmuA__thumb/img/veqFeP4d_3zNzMthSW1Yimgh5mg=/fit-in/200x150/filters:strip_icc()/pic2437871.jpg', 1, 4, 120, 3.86, 'Euro-inspired tactical combat in a persistent world.', ARRAY['Adventure', 'Fantasy'], ARRAY['Cooperative Game', 'Hand Management'], 2, 55000, 8.46, NOW()),
(342942, 'Ark Nova', 2021, 'https://cf.geekdo-images.com/SoU8p28Sk1s8MSvoM4N8pQ__thumb/img/a9vVvIgShiwxNrSE3eLUUqTinbI=/fit-in/200x150/filters:strip_icc()/pic6293412.jpg', 1, 4, 150, 3.70, 'Plan and build a modern zoo to support conservation.', ARRAY['Animals'], ARRAY['Hand Management', 'Set Collection'], 3, 42000, 8.54, NOW()),
(167791, 'Terraforming Mars', 2016, 'https://cf.geekdo-images.com/wg9oOLcsKvDesSUdZQ4rxw__thumb/img/BTxqxgYay5tHJLbugfTv-O0SFqc=/fit-in/200x150/filters:strip_icc()/pic3536616.jpg', 1, 5, 120, 3.24, 'Compete to terraform Mars with corporations.', ARRAY['Economic', 'Science Fiction'], ARRAY['Card Drafting', 'Tile Placement'], 4, 95000, 8.38, NOW()),
(316554, 'Dune: Imperium', 2020, 'https://cf.geekdo-images.com/PhjygpWSo-0labGrPBMyyg__thumb/img/mU-Fn8i0jYfKxC9D0YiI7J4VOMU=/fit-in/200x150/filters:strip_icc()/pic5666597.jpg', 1, 4, 120, 3.01, 'Deck-building and worker placement in the Dune universe.', ARRAY['Science Fiction'], ARRAY['Deck Building', 'Worker Placement'], 5, 48000, 8.36, NOW()),
(266192, 'Wingspan', 2019, 'https://cf.geekdo-images.com/yLZJCVLlIx4c7eJEWUNJ7w__thumb/img/VNToqgS2-pOGU6MuvIkMPKn_y-s=/fit-in/200x150/filters:strip_icc()/pic4458123.jpg', 1, 5, 70, 2.44, 'Engine-building game about bird collection.', ARRAY['Animals', 'Card Game'], ARRAY['Dice Rolling', 'Set Collection'], 6, 88000, 8.06, NOW()),
(169786, 'Scythe', 2016, 'https://cf.geekdo-images.com/7k_nOxpO9OGIjhLq2BUZdA__thumb/img/eQ821VqdXp3C4-Bshl_yNA2YLDY=/fit-in/200x150/filters:strip_icc()/pic3163924.jpg', 1, 5, 115, 3.42, 'Alternate-history 1920s Europe with mechs.', ARRAY['Economic', 'Science Fiction'], ARRAY['Area Control', 'Grid Movement'], 7, 75000, 8.18, NOW()),
(162886, 'Spirit Island', 2017, 'https://cf.geekdo-images.com/kjCm4ZvPjIZxS-mYgSPy1g__thumb/img/YABXmqKz_sw8H1P2c-8Rk6Jt2fo=/fit-in/200x150/filters:strip_icc()/pic3615739.jpg', 1, 4, 120, 4.05, 'Spirits defend their island from colonizers.', ARRAY['Fantasy', 'Fighting'], ARRAY['Cooperative Game', 'Hand Management'], 8, 52000, 8.27, NOW()),
(233078, 'Twilight Imperium: Fourth Edition', 2017, 'https://cf.geekdo-images.com/op8ixq6lnJyMUlxqgDnZSQ__thumb/img/scIjLBSTYKRGS8REccOMOqeDvo8=/fit-in/200x150/filters:strip_icc()/pic3727516.jpg', 3, 6, 480, 4.22, 'Build an intergalactic empire.', ARRAY['Science Fiction', 'Wargame'], ARRAY['Area Control', 'Trading', 'Voting'], 9, 28000, 8.61, NOW()),
(161936, 'Pandemic Legacy: Season 1', 2015, 'https://cf.geekdo-images.com/QHmcoqXCW7vuuSIbJaoCmQ__thumb/img/6qvANLsfVKoWVNJTaOzQLh3djsM=/fit-in/200x150/filters:strip_icc()/pic2452831.png', 2, 4, 60, 2.84, 'Cooperative legacy game fighting diseases.', ARRAY['Medical'], ARRAY['Cooperative Game', 'Hand Management'], 10, 48000, 8.55, NOW())
ON CONFLICT (bgg_id) DO UPDATE SET
  name = EXCLUDED.name,
  year_published = EXCLUDED.year_published,
  thumbnail_url = EXCLUDED.thumbnail_url,
  min_players = EXCLUDED.min_players,
  max_players = EXCLUDED.max_players,
  playing_time = EXCLUDED.playing_time,
  weight = EXCLUDED.weight,
  description = EXCLUDED.description,
  categories = EXCLUDED.categories,
  mechanics = EXCLUDED.mechanics,
  bgg_rank = EXCLUDED.bgg_rank,
  num_ratings = EXCLUDED.num_ratings,
  average_rating = EXCLUDED.average_rating,
  cached_at = NOW();
