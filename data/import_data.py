"""
Import sample data for classification engine
"""

import predictionio
import argparse

def import_events(client, source, result):
  f = open(source, 'r')
  r = open(result, 'r')
  count = 0
  print "Importing data..."

  first_line = True
  results = []

  for line in r:
    if first_line:
      first_line = False
      continue
    data = line.rstrip('\r\n').split(",")
    results.append(data[2]) # end use

  consumptions = []

  first_line = True

  for line in f:
    data = line.rstrip('\r\n').split(",")
    if first_line:
      first_line = False
      for idx in range(0, len(data) - 1):
        consumptions.append([])
      count = len(data) - 1
      continue
    timestamp = data[0]
    
    for idx in range(0, len(data) - 1):
      if not data[idx + 1]:
        continue
      consumptions[idx].append(float(data[idx + 1]))

  for idx in range(0, count):
    arr = consumptions[idx]
    mean = sum(arr) / len(arr)
    variances = map(lambda x : (x - mean) * (x - mean), arr)
    variance = sum(variances)
    client.create_event(
      event="data",
      entity_type="energy",
      entity_id=str(idx),

      properties= {
        "id" : idx,
        "mean" : mean,
        "variance" : variance,
        "enduse" : results[idx],
      }
    )

  f.close()
  print "%s events are imported." % count

if __name__ == '__main__':
  parser = argparse.ArgumentParser(
    description="Import data for circuit enduse prediction engine")
  parser.add_argument('--access_key', default='invalid access key')
  parser.add_argument('--url', default="http://localhost:7070")
  parser.add_argument('--data', default="./data/sample_data.csv")
  parser.add_argument('--result', default="./data/sample_enduse.csv")

  args = parser.parse_args()
  print args

  client = predictionio.EventClient(
    access_key=args.access_key,
    url=args.url,
    threads=5,
    qsize=500)
  import_events(client, args.data, args.result)
