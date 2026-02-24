-- MySQL dump 10.13  Distrib 8.4.8, for Linux (x86_64)
--
-- Host: 35.233.243.215    Database: gamedaydb
-- ------------------------------------------------------
-- Server version	5.7.44-google-log

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
-- GTID statements removed for clean import

--
-- Table structure for table `board_games`
--

DROP TABLE IF EXISTS `board_games`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `board_games` (
  `id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(160) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(400) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `host` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `location` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `max_seats` int(11) NOT NULL DEFAULT '4',
  `start_time` time NOT NULL DEFAULT '21:00:00',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `board_games`
--

LOCK TABLES `board_games` WRITE;
/*!40000 ALTER TABLE `board_games` DISABLE KEYS */;
INSERT INTO `board_games` VALUES ('077b9e2c-0225-4603-ab33-a5ee89d3c066','Books of Time','https://boardgamegeek.com/boardgame/372631/books-of-time','Sarah','TBD',3,'11:30:00','2026-01-29 16:54:13.030460'),('1f9e9f88-f4bf-4c08-b206-abf09e2dae87','Coimbra','https://boardgamegeek.com/boardgame/245638/coimbra','Dale',NULL,4,'13:00:00','2026-02-16 20:12:28.651907'),('3b6adec6-6ea3-4a51-ae56-1ac78308c0cb','Terraformoing Mars','https://boardgamegeek.com/boardgame/167791/terraforming-mars','Andy','TBD',4,'14:00:00','2026-01-29 17:01:49.863527'),('6fd93f4a-89e4-4239-bae3-58004cef88da','Calico','https://boardgamegeek.com/boardgame/283155/calico','Debbie Kushner','Tbd',4,'14:00:00','2026-02-13 20:05:46.374660'),('700a53d7-4921-4719-995a-40207c2eda4c','Machi Koro','https://boardgamegeek.com/boardgame/143884/machi-koro','Ray Simpson','Tbd',5,'14:00:00','2026-02-14 00:22:14.587005'),('75f688b7-310d-4a7c-a415-fe81bcd38f08','Akropolis','https://boardgamegeek.com/boardgame/357563/akropolis','Michelle','TBD',3,'11:30:00','2026-01-29 16:56:27.872739'),('8a1a1fc7-64b5-42c5-9a60-68e8b1119ce8','The White Castle','https://boardgamegeek.com/boardgame/371942/the-white-castle','Andy Wahaus',NULL,3,'11:30:00','2026-01-25 23:06:06.798012'),('a09a0565-6f18-419c-8d46-61ebee5f3626','Crisis','https://boardgamegeek.com/boardgame/128721/crisis','Adam','TBD',3,'11:30:00','2026-01-29 16:52:19.832836'),('b13b913f-c3e0-4077-846e-06992691fc64','Ark Nova','https://boardgamegeek.com/boardgame/342942/ark-nova','Ray Simpson','Tbd',4,'11:00:00','2026-02-14 00:20:24.628392'),('b21136a7-5780-47c7-b778-27a9bc985806','Silver','https://boardgamegeek.com/boardgame/278553/silver','Debbie Kushner',NULL,4,'16:00:00','2026-02-14 00:25:03.357785'),('bd5efae0-64da-467a-ab26-c1a87d391c5d','Consumption Food and Choices','https://boardgamegeek.com/boardgame/198517/consumption-food-and-choices','Adam','TBD',3,'14:00:00','2026-01-29 16:58:24.057652'),('cbaa2719-a92b-401c-9752-c36b82394cb8','Ginkgopolis','https://boardgamegeek.com/boardgame/128271/ginkgopolis','Sarah','TBD',3,'13:00:00','2026-01-29 16:59:42.253677'),('d62ba907-c824-4cb4-828a-b7fc477144dc','Stone Age Anniversary','https://boardgamegeek.com/boardgame/260678/stone-age-anniversary','Jason','TBD',4,'12:00:00','2026-01-29 17:05:54.207117'),('e7c79898-cd09-4460-adcb-eb8a8de9d64f','Coal Baron','https://boardgamegeek.com/boardgame/143515/coal-baron','Susan','TBD',3,'15:00:00','2026-01-29 17:01:06.757794'),('ea66eedc-248c-423d-bc44-56cf5b8a9b56','ItΓÇÖs a wonderful world',NULL,'Andy b',NULL,4,'13:00:00','2026-02-14 05:07:43.293753'),('f036de6f-92d6-4409-b53e-73da6ce2bc3b','Evergreen','https://boardgamegeek.com/boardgame/363307/evergreen','Andy','TBD',3,'18:00:00','2026-01-29 17:03:38.028332'),('f926f51d-956b-46c3-b6fc-bced285a95c8','Underwater Cities','https://boardgamegeek.com/boardgame/247763/underwater-cities','Adam','1',3,'17:00:00','2025-11-21 18:16:18.660235');
/*!40000 ALTER TABLE `board_games` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `reservations`
--

DROP TABLE IF EXISTS `reservations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reservations` (
  `id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `board_game_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(160) COLLATE utf8mb4_unicode_ci NOT NULL,
  `reserved_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_reservation_email` (`board_game_id`,`email`),
  CONSTRAINT `fk_reservation_game` FOREIGN KEY (`board_game_id`) REFERENCES `board_games` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reservations`
--

LOCK TABLES `reservations` WRITE;
/*!40000 ALTER TABLE `reservations` DISABLE KEYS */;
INSERT INTO `reservations` VALUES ('1d39182b-e878-4334-a9aa-3e9f6d38aedc','b21136a7-5780-47c7-b778-27a9bc985806','Debbie Kushner','debbie.kushner93@gmail.com','2026-02-14 00:25:17.642811'),('22090d9c-ead8-40d0-9abf-64552d44786a','6fd93f4a-89e4-4239-bae3-58004cef88da','Debbie Kushner','debbie.kushner93@gmail.com','2026-02-13 20:06:17.425659'),('397f0ecd-f895-43e0-9364-054523842862','b13b913f-c3e0-4077-846e-06992691fc64','Ray Simpson','ray@just1label.com','2026-02-14 00:25:57.512754'),('3d98767c-ba31-4736-8685-3526f3bba3b4','b21136a7-5780-47c7-b778-27a9bc985806','Ray Simpson','ray@just1label.com','2026-02-14 00:26:40.134669'),('70ddbdec-2702-47e8-8359-1e4b62861bf3','700a53d7-4921-4719-995a-40207c2eda4c','Ray Simpson','ray@just1label.com','2026-02-14 00:25:36.172312'),('77f22144-d23b-4b78-91cd-564ba791aae0','8a1a1fc7-64b5-42c5-9a60-68e8b1119ce8','Nicole Rubenstein','nicolealir@gmail.com','2026-02-16 04:35:14.612658'),('881141db-e621-42f7-ba72-d6c4f0f00e7e','3b6adec6-6ea3-4a51-ae56-1ac78308c0cb','Shan','gforce11@gmail.com','2026-02-07 17:46:06.037371'),('d2d97f60-e72b-434b-82f9-a379882c09c7','077b9e2c-0225-4603-ab33-a5ee89d3c066','Debbie Kushner','debbie.kushner93@gmail.com','2026-02-13 20:07:00.538234'),('dee9f14b-fe39-462a-a575-9b0f22a2b2f4','f926f51d-956b-46c3-b6fc-bced285a95c8','Debbie Kushner','debbie.kushner93@gmail.com','2026-02-14 00:16:04.468420'),('eb3283c4-61be-4b8b-be67-45eb1831d00d','077b9e2c-0225-4603-ab33-a5ee89d3c066','Shan','gforce11@gmail.com','2026-02-07 17:52:45.517729'),('fbd493a9-b776-4ccb-9b92-c9d7b21ae531','f926f51d-956b-46c3-b6fc-bced285a95c8','Ray Simpson','ray@just1label.com','2026-02-14 00:16:44.664457');
/*!40000 ALTER TABLE `reservations` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-02-18  4:45:50
