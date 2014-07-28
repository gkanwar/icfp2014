/*
This will pre process ghc files to produce real ghc files

It current has these features:
labels
vars
function calling

*/

#include <iostream>
#include <vector>
#include <string>

using namespace std;

vector<string> labels;
vector<int> labelLines;
vector<string> lines;
vector<string> vars;
vector<int> varNums;

int curVarNum = 0;
int lineNum = 0;
int curFuncNum  = 0;

bool has_only_spaces(const string& str) {
  int firstNotSpace = str.find_first_not_of (' ');
  if ( firstNotSpace == str.npos ) {
    return true;
  }
  else if ( str.at(firstNotSpace) == ';') {
    return true;
  }
  return false;
}

string myreplace(string &s,
		 string toReplace,
		 string replaceWith)
{
  int replacePos = s.find(toReplace);
  if ( replacePos != -1) {
    if (replacePos > 0 && isalpha(s[replacePos-1])) {
      return s;
    }
    else if ( replacePos + toReplace.size() < s.size() - 1 && isalpha(s[replacePos+toReplace.size()])){
      return s;
    }
    return(s.replace(replacePos, toReplace.length(), replaceWith));
  }
  return s;
}

bool isVar(const string& toCheck) {
  return toCheck.find("var") != toCheck.npos;
}

string getVar(const string& varLine) {
  return varLine.substr(4);
}

bool isFunc(const string& toCheck) {
  return toCheck.find("()") != toCheck.npos;
}

void pushFunc(string line) {
  int funcEnd = line.find("()");
  string funcName = line.substr(0,funcEnd);
  string returnLine = "mov h," + to_string(lineNum + 2);
  string jumpLine = "jeq " + funcName + ",0,0";
  lines.push_back(returnLine);
  lines.push_back(jumpLine);
  lineNum+=2;
}

bool isReturn(const string& toCheck) {
  return toCheck.find("return") != toCheck.npos;
}

void createVar(string line) {
  string newVar = getVar(line);
  vars.push_back(newVar);
  if ( curVarNum < 255 ) {
    varNums.push_back(curVarNum++);
  }
  else {
    cerr << "Error: too many variables" << endl;
  }
}
int main() {
  while(!cin.eof()) {
    if (lineNum > 255) {
      cerr << "Error: too many lines of code";
    }
    string curLine;
    getline(cin, curLine);
    if ( curLine.back() == ':') {
      curLine.resize(curLine.size()-1);
      labels.push_back(curLine);
      labelLines.push_back(lineNum);
    }
    else if (isVar(curLine)) {
      createVar(curLine);
    }
    else if (isFunc(curLine)) {
      pushFunc(curLine);
    }
    else if (isReturn(curLine)) {
      lines.push_back("mov pc,h");
      lineNum++;
    }
    else if (!has_only_spaces(curLine)){
      lines.push_back(curLine);
      lineNum++;
    }
  }
  
  for ( int i = 0; i < lines.size(); i++ )  {
    string toOutput = lines.at(i);
    for ( int j = labels.size(); --j >=0; ) {
      toOutput = myreplace(toOutput, labels.at(j), to_string(labelLines.at(j)));
    }
    for ( int j = vars.size(); --j >=0; ) {
      toOutput = myreplace(toOutput, vars.at(j), to_string(varNums.at(j)));
    }
    toOutput = myreplace(toOutput, "mv","mov");
    cout << toOutput << endl;
  }
  cerr << "currently " << lineNum << " lines of code" << endl;
}
