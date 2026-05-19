import os

base_dir = r"d:\CuoiKiJava\auction-app"
structure = {
    "src/main/java/server": ["AuctionServer.java", "ClientHandler.java", "AuctionRoom.java", "AuctionManager.java", "SessionManager.java"],
    "src/main/java/client": ["MainFrame.java", "LoginPanel.java", "RegisterPanel.java", "UserDashboard.java", "ModeratorDashboard.java", "AdminDashboard.java", "ChatPanel.java", "AuctionRoomPanel.java", "ProductSubmitPanel.java", "HistoryPanel.java"],
    "src/main/java/shared": ["XmlMessageParser.java", "MessageType.java"],
    "src/main/java/dao": ["UserDAO.java", "ProductDAO.java", "AuctionSessionDAO.java", "BidDAO.java", "ChatDAO.java", "PenaltyDAO.java"],
    "src/main/java/model": ["User.java", "Product.java", "AuctionSession.java", "Bid.java", "ChatMessage.java", "Penalty.java"],
    "src/main/resources": ["schema.sql"]
}

for folder, files in structure.items():
    folder_path = os.path.join(base_dir, folder)
    os.makedirs(folder_path, exist_ok=True)
    pkg = folder.split("/")[-1]
    
    for file in files:
        file_path = os.path.join(folder_path, file)
        if not os.path.exists(file_path):
            with open(file_path, "w", encoding="utf-8") as f:
                if file.endswith(".java"):
                    class_name = file[:-5]
                    is_enum = class_name == "MessageType"
                    type_str = "enum" if is_enum else "class"
                    f.write(f"package {pkg};\n\npublic {type_str} {class_name} {{\n\n}}\n")
                else:
                    f.write("-- Database schema goes here\n")

print("Generated directories and files successfully.")
