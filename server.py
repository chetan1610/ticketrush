import json
from http.server import BaseHTTPRequestHandler,HTTPServer


SHOWS=[
     {"id": 1, "movie": "Inception", "seats_left": 5},
    {"id": 2, "movie": "Interstellar", "seats_left": 2},
]

BOOKINGS=[]


class TicketHandler(BaseHTTPRequestHandler):

    def send_json(self, status, data):
        body = json.dumps(data).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)
        
    def find_show(self, show_id):
        for show in SHOWS:
            if show["id"] == show_id:
                return show
        return None
    
    
    def do_GET(self):
        if self.path=="/shows":
            self.send_json(200,SHOWS)
            
        elif self.path.startswith("/shows/"):
            try:
                show_id=int(self.path.split("/")[2])
            except ValueError:
                return self.send_json(400,{"error":"show not found"})
            
            show=self.find_show(show_id)
            if show is None:
                return self.send_json(404,{"error":"show not found"})
            seld.send_json(200,show)
            
        else:
            self.send_json(404,{"error":"rote not found"})
            
            
        def do_POST(self):
            if self.path!="/bookings":
                return self.send_json(404, {"error": "route not found"})
            
            length=int(self.header.get("Content-Length",0))
            raw_body=self.rfile.read(length)
            
            try:
                data=json.loads(raw_body)
                show_id=data["show_id"]
                seats=data["seats"]
            except (json.JSONDecoderError,KeyError):
                return self.send_json(400, {"error": "send JSON with show_id and seats"})
            
            show = self.find_show(show_id)
            if show is None:
                return self.send_json(404, {"error": "show not found"})
            
            if seats > show["seats_left"]:
                return self.send_json(409, {"error": "not enough seats"})
            
            
            show["seats_left"]-=seats
            booking={"id":len(BOOKINGS)+1,"show_id":show_id,"seats":seats}
            BOOKINGS.append(booking)
            self.send_json(201,booking)
            
            
            
if __name__=="__main__":
    server=HTTPServer(("127.0.0.1", 8000), TicketHandler)
    print("TicketRush running on http://127.0.0.1:8000")
    server.serve_forever()
                
                