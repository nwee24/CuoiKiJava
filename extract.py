import json
import re

transcript_path = r"C:\Users\ACER\.gemini\antigravity-ide\brain\03afe66b-c225-45f5-ab21-33178052ad93\.system_generated\logs\transcript.jsonl"
output_path = r"d:\CuoiKiJava\CuoiKiJava\src\main\java\server\ClientHandler.java"

content_blocks = {}

with open(transcript_path, "r", encoding="utf-8") as f:
    for line in f:
        try:
            d = json.loads(line)
            if "output" in d:
                out = d["output"]
                if "File Path: `file:///d:/CuoiKiJava/CuoiKiJava/src/main/java/server/ClientHandler.java`" in out:
                    # extract lines
                    lines = out.split("\n")
                    for l in lines:
                        match = re.match(r"^(\d+):\s(.*)", l)
                        if match:
                            line_num = int(match.group(1))
                            line_content = match.group(2)
                            content_blocks[line_num] = line_content
        except Exception as e:
            pass

if content_blocks:
    max_line = max(content_blocks.keys())
    print(f"Recovered up to line {max_line}")
    with open(output_path, "w", encoding="utf-8") as f:
        for i in range(1, max_line + 1):
            if i in content_blocks:
                f.write(content_blocks[i] + "\n")
            else:
                f.write("\n")
else:
    print("Could not find any content for ClientHandler.java in transcript.")
