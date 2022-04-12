import argparse
from email import header
import pathlib
import os


def handle_args():
    parser = argparse.ArgumentParser()
    parser.add_argument("file", help="logfile to be extracted.")
    parser.add_argument("outfile", help="output destination")
    parser.add_argument("--keywords", "-k", action="append", help="Keywords which should be in a line")
    parser.add_argument("--label", "-l", action="append", help="Specifies label", type=str)
    parser.add_argument("--force", "-f", action="store_true", help="overwrite existing files")

    return parser.parse_args()


def is_parseable_line(line, keywords):
    for keyword in keywords:
        if keyword.lower() in line.lower():
            return True

    return False


def split_line(line):
    data = line.split(";")

    sensor_name = data[0]
    timestamp = data[1]
    values = data[2:]
    return sensor_name, timestamp, values


def main():
    args = handle_args()

    file = pathlib.Path(args.file)
    if not file.exists():
        print(f"File {file} does not exist.")
        return

    outfile = pathlib.Path(args.outfile)
    if not outfile.exists():
        outfile.touch()
    #     print(f"Outfile {outfile} does already exist.")
    #     return

    lines = file.read_text()

    output_data = {}
    final_output = []

    # create the correct header
    for line in lines.splitlines():
        if not is_parseable_line(line, args.keywords):
            continue
        name, _, values = split_line(line)
        output_data[name] = ["0"] * (len(values) - 1)

    # create the header string
    # header_string = ""
    # for key, value in output_data.items():
    #     header_string += (f"{key};")
    #     for index in range(len(value)):
    #         header_string += (f"{index};")
    # header_string += os.linesep
    # final_output.append(header_string)

    # fill in the data
    for line in lines.splitlines():
        name, _, values = split_line(line)
        output_data[name] = values

        data_string = ""
        for _, value in output_data.items():
            data_string += ",".join(value)

        if (len(value) == 3):
            data_string += ","
        data_string += ",".join(args.label)
        #data_string += ","
        data_string += "\n"

        final_output.append(data_string)

    if not args.force:
        with open(outfile, "a") as out:
            for line in final_output:
                out.write(line)
    else:
        with open(outfile, "w") as out:
            for line in final_output:
                out.write(line)


if __name__ == "__main__":
    main()
